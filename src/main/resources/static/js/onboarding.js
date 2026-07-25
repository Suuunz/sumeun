// 온보딩: 2단계 취향 선택 → 완료/건너뛰기 시 지도로 이동
let currentStep = 1;
const selections = { 1: false, 2: false };

function selectCard(step, element) {
    document.querySelectorAll(`#step-${step} .option-card`).forEach((c) => c.classList.remove('selected'));
    element.classList.add('selected');
    selections[step] = true;
    document.getElementById('next-btn').disabled = false;

    if (step === 1) {
        setTimeout(nextStep, 400);
    }
}

function nextStep() {
    if (currentStep === 1 && selections[1]) {
        document.getElementById('step-1').classList.replace('step-visible', 'step-hidden');
        setTimeout(() => {
            document.getElementById('step-2').classList.replace('step-hidden', 'step-visible');
        }, 50);

        document.getElementById('progress-2').classList.remove('bg-border');
        document.getElementById('progress-2').classList.add('bg-primary-container');

        const nextBtn = document.getElementById('next-btn');
        nextBtn.innerText = '시작하기';
        nextBtn.disabled = !selections[2];
        currentStep = 2;
    } else if (currentStep === 2 && selections[2]) {
        // 완료 → 지도 (지침 10)
        window.location.href = '/map';
    }
}

function skip() {
    // 건너뛰기 → 지도
    window.location.href = '/map';
}
