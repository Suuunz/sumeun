// Figma 반입용 데모 데이터 — 실제 화면 흐름을 그대로 거쳐 만든다.
// (직접 INSERT 하지 않는 이유: 실제 저장 경로를 통과해야 화면과 어긋나지 않는다)
import fs from "node:fs";

const B = "http://localhost:8080";
let cookie = "";

async function req(p, i = {}) {
    const r = await fetch(B + p, {
        ...i,
        headers: { ...(i.headers || {}), ...(cookie ? { cookie } : {}) },
        redirect: "manual",
    });
    const s = r.headers.getSetCookie?.().find((c) => c.startsWith("JSESSIONID="));
    if (s) cookie = s.split(";")[0];
    return r;
}
const csrf = async (p) => ((await (await req(p)).text()).match(/name="_csrf"\s+value="([^"]+)"/) || [])[1];
const form = (o) => new URLSearchParams(o);
const H = { "content-type": "application/x-www-form-urlencoded; charset=UTF-8" };

/* 데모 계정 */
await req("/");
let token = await csrf("/signup");
let r = await req("/signup", { method: "POST", headers: H, body: form({
    email: "demo@sumeun.kr", password: "sumeun1234", confirmPassword: "sumeun1234",
    nickname: "여행자 민서", _csrf: token }) });
console.log("가입:", r.status, r.headers.get("location"));

/* 프로필 소개 */
token = await csrf("/profile");
await req("/profile", { method: "POST", body: (() => {
    const fd = new FormData();
    fd.append("nickname", "여행자 민서");
    fd.append("bio", "사람 적은 바닷가 마을과 오래된 골목을 좋아합니다.");
    fd.append("_csrf", token);
    return fd;
})() });

/* 코스 — 안동, 좌표 있는 실제 관광지 3곳 */
token = await csrf("/course?sigCd=47170");
const stops = [
    { name: "월영교", type: "attraction", sage: false, lat: 36.5580, lng: 128.7180 },
    { name: "안동찜닭골목", type: "goodprice", sage: true, lat: 36.5665, lng: 128.7290 },
    { name: "안동민속촌", type: "attraction", sage: false, lat: 36.5719, lng: 128.7462 },
];
r = await req("/course/save", { method: "POST", headers: H, body: form({
    sigCd: "47170", courseName: "느리게 걷는 안동 하루", itemsJson: JSON.stringify(stops), _csrf: token }) });
const courseId = (r.headers.get("location") || "").split("courseId=")[1];
console.log("코스:", courseId);

/* 경로 계산(거리·시간이 채워진 저장완료 화면을 만들기 위해) */
const route = await (await req(`/api/course/${courseId}/route`)).json();
console.log("경로:", route.distanceText, route.durationText);

/* 후기 — 사진 포함, 공개 */
const photo = fs.readFileSync("demo-photo.png");
token = await csrf(`/review/new?courseId=${courseId}`);
const fd = new FormData();
fd.append("courseId", courseId);
fd.append("content",
    "월영교는 해질 무렵에 가는 게 좋았어요. 다리 위에서 보는 노을이 오래 기억에 남습니다.\n" +
    "찜닭골목은 평일 저녁에도 자리가 있었고, 민속촌은 사람이 적어 천천히 둘러보기 좋았어요.");
fd.append("shared", "true");
fd.append("photos", new Blob([photo], { type: "image/png" }), "andong.png");
fd.append("_csrf", token);
r = await req("/review", { method: "POST", body: fd });
console.log("후기:", r.status, r.headers.get("location"));

console.log("\n데모 계정: demo@sumeun.kr / sumeun1234");
