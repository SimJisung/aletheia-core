# 과거 생각 파편 기반 자기일관성 및 후회 최소화 AI 시스템: 이론적 배경 조사

> **문서 버전**: 1.1 (개정판)
> **최종 수정**: 2026-01-26
> **프로젝트**: Aletheia-Core (Mnemosyne-Protocol)

---

## 개요

사용자의 과거 **"생각 파편(Thought Fragment)"**을 토대로 의사결정에서 자기 일관성(self-consistency) 유지와 후회 최소화(regret minimization)를 지원하는 AI 시스템 아키텍처가 제시되었다. 해당 시스템은 다음 세 가지 핵심 요소로 구성된다:

| 구성요소 | 설명 |
|---------|------|
| **Thought Fragment Store** | 사용자의 과거 발언, 생각 조각, 메모 등을 원형과 감정 점수/시간정보와 함께 저장 |
| **Value Graph Engine** | 축적된 파편들을 가치 축에 따라 시맨틱 그래프로 재구성하고 시간/맥락 기반 연관성과 감정 편차를 해석 (가치 간 모순 허용) |
| **Decision Projection Engine** | 현재 의사결정 시 과거 유사한 가치 기반 발언을 검색하여 해당 선택의 일치도를 확률로 예측 (Bayesian+heuristic)하며, LLM은 설명 생성에만 사용됨 |

이 보고서에서는 이러한 시스템을 이론적으로 뒷받침할 수 있는 주요 논문들과 학문적 프레임워크를 심리학, 인지과학, 신경과학, 행동경제학, 메타인지/자기성찰의 영역별로 조사하였다.

---

## 1. 심리학 분야 주요 이론 및 논문

### 1.1 Self-Consistency Theory
**Prescott Lecky, 1945**

개인은 자기 개념과 행동 간의 일관성을 유지하려는 근본적 동기에 의해 행동한다고 주장한다[^1]. 자신이 보는 *자기상(self-concept)*과 모순되는 정보나 행동이 발생하면 심리적 불편감이 생기며, 이를 해소하기 위해 인지적 왜곡이나 합리화 등 방어기제를 사용해 자기 일관성을 복원한다[^2]. Lecky는 "모든 행위는 동일한 가치 구조를 유지하려는 목표를 가진다"고 설명했는데, 이는 사람들이 일관된 핵심 가치 구조를 지키며 행동하려 함을 뜻한다.

**적용**: 사용자의 과거 가치관과 자기 개념을 보존하는 **Thought Fragment Store** 및 **Decision Projection Engine** 설계에 영감을 준다. 시스템이 개인의 발언과 행동을 일관되게 유지하도록 과거 생각 파편을 참고하는 원리가 Lecky의 자기일관성 이론에 부합한다.

---

### 1.2 Cognitive Dissonance Theory
**Leon Festinger, 1957**

사람들은 서로 모순된 인지(예: "난 환경을 중시해" vs "플라스틱 용기를 사용해")를 동시에 지니면 불편한 긴장 상태인 인지 부조화를 느끼며, 이를 줄이기 위해 신념이나 행동을 변경한다[^3]. Festinger에 따르면 우리의 의견과 태도는 내부적으로 일관된 군집을 이루려는 경향이 있으나, 때때로 생기는 불일치는 두드러지게 인지되어 관심을 끈다.

**적용**: **Value Graph Engine**이 모순된 가치들을 한 그래프 내에 허용하고 표시하는 설계에 이론적 근거를 제공한다. 사용자의 가치 간 충돌을 시맨틱 그래프로 드러내어 인지부조화를 인식하게 하고, **Decision Projection Engine**이 이러한 갈등을 고려하여 더 만족스러운(부조화가 적은) 의사결정을 추천하도록 돕는다.

---

### 1.3 Regret Theory (후회 이론)
**Graham Loomes & Robert Sugden, 1982**

전통적 기대효용 이론과 달리, 후회 이론은 의사결정 시 사람들이 미래에 느낄 후회를 미리 예상하여 선택에 반영한다고 설명한다[^4]. 즉 결정 결과에 대한 감정적 반응인 후회를 단순 사후감정이 아니라, 사람들이 의사결정 과정에서 능동적으로 예견하여 가능한 한 후회가 적도록 선택을 조정하는 요인으로 본다.

Loomes와 Sugden은 이러한 후회를 효용함수에 후회 항으로 포함시켜 이론화하였고, 선택으로 얻은 실제 결과와 이후 밝혀진 최선의 선택 결과 간의 가치 차이를 후회의 정도로 정의하였다[^5]. 그 결과 후회 회피(regret aversion) 성향 때문에 사람들이 순수한 기대값 극대화와 다른 의사결정을 내리는 현상을 설명한다.

**적용**: **Decision Projection Engine**의 알고리즘에 후회 최소화 전략을 포함시키는 이론적 근거를 제공한다. 현재 선택으로 인한 미래 후회를 예측(예: 대안 결과와의 비교)하여 후회가 가장 적을 행동을 추천하는 기능은 이 이론에 기반한다.

---

### 1.4 Emotion and Memory Recall (감정과 기억 회상)
**Gordon H. Bower, 1981**

기분-일치 기억(mood-congruent memory) 현상에 따르면, 사람들은 현재의 감정 상태와 일치하는 정서적 색채의 기억을 더 잘 떠올린다[^6]. 예컨대 우울할 때는 과거의 부정적 경험이 더 많이 회상되고, 행복할 때는 긍정적 기억이 더 잘 떠오른다. 이는 정서가 기억의 연상 네트워크를 활성화하여, 현재 기분과 톤이 맞는 정보에 주의와 인지자원을 더 쓰기 때문인 것으로 설명된다.

**적용 (수정됨)**:
> **주의**: Bower의 연구는 인간의 *자연 발생적* 기억 편향을 설명한다. 시스템이 이를 *의도적으로 구현*하면 오히려 편향을 강화할 위험이 있다.

따라서 **Thought Fragment Store**에서 감정 점수를 저장하되, **Decision Projection Engine**은 현재 감정과 *일치하는* 파편만 검색하기보다 **감정 편향을 인지하도록 돕는** 방향으로 설계해야 한다. 예: "현재 기분이 부정적이라 부정적 기억이 더 떠오를 수 있습니다. 균형 잡힌 판단을 위해 다양한 맥락의 과거 경험을 함께 고려하세요."

---

## 2. 인지과학 및 인공지능 분야 주요 연구

### 2.1 Case-Based Decision Theory (사례 기반 의사결정 이론)
**Itzhak Gilboa & David Schmeidler, 1995**

사람들은 과거에 직면했던 유사한 사례를 바탕으로 새로운 의사결정을 내린다는 대안 이론이다[^7]. Gilboa 등은 결정 상황에서 인간이 과거 *사례(case)*를 원형으로 저장해두었다가, 현재 상황과 비슷한 과거 사례의 결과를 떠올려 **유추 기반(choice by analogy)**으로 선택한다고 모델링했다. 예를 들어, 과거에 비슷한 선택으로 성공을 거둔 행동을 현재도 취하는 경향이 있다는 것이다.

**적용**: **Decision Projection Engine**의 기본 개념과 부합하며, **Thought Fragment Store**에 축적된 과거 사례를 현재 결정에 활용하는 핵심 논리를 제공한다. 시스템이 현재 의사결정 요청을 받으면, 과거의 유사 맥락 발언/결정 사례를 검색하여 그 결과(만족도나 후회 여부)를 참고하고 현재 옵션의 예상 적합도를 계산하는 아이디어가 이 이론에 영감을 받았다.

---

### 2.2 Episodic Memory & Value-Based Decision
**Katherine D. Duncan & Daphna Shohamy, 2016**

인지심리학 실험을 통해 **일화기억(episodic memory)**이 가치 기반 선택에 미치는 영향을 규명한 연구이다[^8]. 익숙한 배경 맥락에서는 사람들이 과거의 구체적 경험 기억을 더 많이 의사결정에 활용하며, 새로운 맥락에서는 과거 기억보다 새 정보 학습에 치중한다는 결과를 보였다. 요컨대, 과거 경험 기억이 선택에 미치는 영향은 맥락의 친숙도에 따라 달라지지만, 전반적으로 기억은 중요한 의사결정 요소임을 밝힌 것이다.

**적용**: 사용자의 과거 에피소드(생각 파편)를 시맨틱 및 시간 맥락으로 연결하여 현재 결정에 제공하는 시스템 설계 전반 (**Thought Fragment Store**와 **Decision Projection Engine**)에 과학적 근거를 준다. 기억이 의사결정 성과를 높일 수 있다는 점은, 시스템이 맥락 의존적인 메모리 검색을 통해 사용자에게 가장 일관되고 유의미한 과거 경험을 투사해주는 기능의 유용성을 뒷받침한다.

---

### 2.3 Cognitive-Affective Maps (인지-정서 지도)
**Paul Thagard et al., 2014**

복잡한 신념 체계와 그에 수반된 감정들을 시각적 그래프 형태로 표현하는 기법이다[^9]. Thagard는 이를 *"가치 지도(value map)"*라고도 부르며, 한 개인이나 집단이 가진 주요 개념, 신념, 목표를 정서적 긍정/부정 가치와 함께 노드(도형)로 나타내고, 개념들 사이의 관계를 서로 보강(지지)하거나 충돌(상충)하는 선으로 연결해 도식화한다.

- 긍정 감정 → 원(oval)
- 부정 감정 → 육각형(hexagon)
- 중립/양가 → 사각형(rectangle)
- 지지 관계 → 실선
- 충돌 관계 → 점선

**적용**: **Value Graph Engine** 설계에 직접적인 영감을 준다. 사용자의 생각 파편을 가치 지향 그래프 형태로 조직하여, 각 노드에 **감정 점수(밸류)**를 부여하고, 상호 지지/충돌 관계를 표현하는 아이디어가 Thagard의 CAM과 유사하다. 특히 시스템이 모순된 가치들도 그래프 내에 공존시킨다는 점은 CAM의 "상충 관계 허용"과 맥을 같이한다.

---

## 3. 신경과학 분야 주요 연구

### 3.1 뇌의 후회 신호: Orbitofrontal Cortex & Regret
**Giorgio Coricelli et al., 2005**

뇌영상(fMRI) 연구를 통해 의사결정 후에 느끼는 후회와 관련된 특정 뇌 부위를 규명한 논문이다[^10]. 위험이 따르는 선택을 한 실험참가자들에게 선택 후 다른 대안을 택했을 경우의 결과까지 알려주었더니, 자신의 선택이 최선이 아니었음을 깨달았을 때 즉 후회를 느낄 때 뇌의 **안와전두피질(OFC)**이 현저히 활성화되었다.

특히 얻은 결과와 다른 선택 시 얻을 수 있었던 최적 결과 간 격차가 클수록 OFC 활동이 강해져, OFC가 후회의 정도를 반영함이 관찰되었다. 또한 선택 결과가 운에 따라 정해져 자신이 통제할 수 없다고 인식한 경우에는 OFC 활성화가 나타나지 않아, 후회 감정에는 자신의 선택에 대한 책임감이 수반될 때 뇌반응이 일어남을 시사했다.

**적용**: **Decision Projection Engine**의 후회 예측 모듈에 대한 신경학적 타당성을 부여한다. 인간의 뇌가 선택 이후 대안 시나리오를 비교하며 후회를 신호로 발생시키고 학습에 활용하듯, 시스템도 후회 값을 계산하여 제공함으로써 사용자가 나중에 후회할 선택을 미리 피하도록 돕는 설계를 정당화한다.

---

### 3.2 뇌의 갈등 모니터링: ACC, DLPFC & Cognitive Dissonance
**Izuma et al., 2010 (PNAS)**

어려운 선택 이후 선호가 변화하는 인지부조화 효과의 신경 상관성을 조사한 연구이다[^11]. 실험에서 피험자들이 두 가지 동등하게 좋아하는 옵션 중 하나를 선택하면, 선택 후에 **선택한 항목을 더 선호하고 버린 항목을 덜 선호하게 되는 변화(선택에 따른 태도 변경)**가 발생함을 확인했다.

뇌영상 분석 결과, 선택 행위만으로도 뇌 보상계의 선호 신호(선조체 활동)가 변형되었을 뿐 아니라, **전대상피질(ACC)**과 **배외측전전두피질(DLPFC)**이 이러한 인지 부조화의 정도를 일시적으로 추적함이 밝혀졌다. 즉, "내가 A를 좋아하지만 A를 버렸다"는 모순이 클수록 ACC와 DLPFC의 활성도가 높아져, 뇌가 해당 가치 충돌을 감지하고 적극적으로 태도를 조정(합리화)하고 있음을 시사한다.

**적용 (수정됨)**: **Value Graph Engine**이 가치 충돌을 표면화하고 **Decision Engine**이 이를 고려하는 체계에 대해 생물학적 근거를 제공한다.

> **주의**: 뇌의 자동적 갈등 감지와 시스템의 명시적 충돌 표시는 다른 메커니즘이다. 사용자에게 충돌을 직접 보여주면 오히려 *방어적 반응(defensive response)*이 나타날 수 있다.

따라서 LLM 설명 생성기는 충돌을 "판단"하기보다 **중립적으로 제시**하고, 사용자가 스스로 성찰하도록 유도하는 방식이 바람직하다.

---

### 3.3 가치 부호화의 신경 기제: Striatum & vmPFC
**Antonio Rangel et al., 2008 등**

신경경제학 연구에 따르면, 인간의 뇌는 선택지의 주관적 가치를 통합하여 평가하는 공통 회로를 갖고 있다[^12]. 주로 **복내측 전전두피질(vmPFC)**과 **선조체(striatum)**가 다양한 선택 상황(금전, 음식, 사회적 보상 등)에서 선택지의 가치를 계산하고 비교하는 활성화를 보인다.

**적용**: 시스템의 **Value Graph Engine**과 **Decision Engine**이 정량적 가치 평가 모형을 도입하는 데 영감을 준다. 인간 뇌가 다양한 속성의 선택지들을 단일 가치 척도로 환산해 비교하듯이, 시스템도 사용자의 가치 그래프를 기반으로 각 옵션의 총합적 가치 점수를 계산하여 비교할 수 있다.

---

## 4. 행동경제학 분야 주요 이론 및 논문

### 4.1 Prospect Theory (전망 이론)
**Daniel Kahneman & Amos Tversky, 1979**

인간의 실제 의사결정 양식을 설명한 행동경제학의 대표 이론이다[^13]. Kahneman 등이 여러 실험을 통해 밝혀낸 바에 따르면, 사람들은 이익과 손실을 동일하게 취급하지 않고 비대칭적으로 평가한다.

- **손실회피(loss aversion)**: 동일 금액의 이득보다 손실에서 느끼는 고통이 훨씬 크게 작용
- **확률 가중치**: 낮은 확률은 과대평가, 높은 확률은 과소평가
- **참고점 의존**: 절대적 가치가 아닌 현재 참고점(reference point) 기준 상대 평가

**적용**: **Decision Projection Engine**이 인간의 가치 함수 및 확률 왜곡을 반영하도록 설계되는 근거가 된다. 단순한 기대값 계산 대신, 시스템은 Prospect Theory에서 제시하는 가치함수와 확률가중 함수 등을 활용해 사용자의 입장에서 각 선택지가 어떻게 지각될지를 평가할 수 있다.

---

### 4.2 Regret Aversion in Decision Making (후회 회피 성향)
**Bell, 1982; Loomes & Sugden, 1982**

개인이 불확실한 선택을 할 때 미래에 덜 후회할 쪽을 선호하는 경향을 정량적으로 표현한다[^14]. 경제학자들은 후회를 고려하는 선택모델에서, 의사결정자의 효용함수에 **"만약 최적 선택을 하지 않았을 때 발생하는 심리적 비용"**을 포함시켰다.

**적용**: **Decision Projection Engine**의 의사결정 기준 함수에 후회 항목을 추가하는 이론적 뒷받침이 된다. 시스템은 사용자의 과거 후회 데이터나 보편적인 후회 값(예: 최선 대비 효용 손실)을 활용하여 **"최대 후회값"**이 최소인 옵션을 계산할 수 있다. 이는 의사결정에서 **최소 후회의 원칙(minimax regret)**을 구현하는 것이다.

---

## 5. 메타인지 및 자기성찰 분야 주요 연구

### 5.1 Reflective vs. Impulsive Systems (반추적 vs. 충동적 시스템)
**Fritz Strack & Roland Deutsch, 2004**

심리학의 이중과정 모델로서, 인간 행동에는 느리고 숙고하는 반추적 시스템과 빠르고 자동적인 충동적 시스템 두 체계가 상호작용한다는 이론이다[^15].

| 시스템 | 특징 |
|--------|------|
| **반추적(Reflective)** | 사실 지식과 가치에 기반해 행동 결정 생성 |
| **충동적(Impulsive)** | 연상과 즉각적 욕구에 따라 행동 유발 |

**적용**: 제시된 AI 시스템 전체가 일종의 외재화된 반추적 시스템으로 기능한다고 볼 수 있다. 특히 **Decision Projection Engine**은 사용자의 충동적 결정 경향을 제어하고 **사실 지식(과거 파편)**과 **가치(그래프)**를 고려하여 신중한 결정을 내리는 System 2 역할을 한다.

---

### 5.2 Metacognitive Self-Monitoring & Self-Regulation
**Carver & Scheier, 1982; 교육학 메타분석**

메타인지는 *"자신의 사고에 대한 생각"*으로, 효과적인 문제 해결과 학습에 필수적인 자기성찰적 모니터링을 포함한다[^16]. Carver와 Scheier는 인간 행동을 목표 대비 피드백 조절로 설명하면서, 스스로 설정한 **기준(목표)**과 현재 행동 사이의 차이를 점검하고 수정해 나가는 피드백 고리가 자기조절의 핵심이라고 보았다.

**적용**: 이 시스템은 사용자의 메타인지적 성찰을 촉진하도록 설계되어 있다. **Thought Fragment Store**에 스스로의 생각을 축적하고 돌아보는 과정, LLM 설명 생성기를 통해 현재 결정과 과거 맥락을 비교해 피드백을 제공하는 과정 모두 사용자의 자기인식 능력을 높이고 더 나은 선택을 하게 돕는다.

---

### 5.3 Metacognitive "Laziness" and Need for Reflection
**Fan et al., 2024 (BJET)**

최신 연구 및 AI 분야 논의에서는, 충분한 자기점검 없이 빠르게 결정을 내려버리는 현상을 *메타인지적 게으름(metacognitive laziness)*이라고 부른다[^17]. 이는 인간뿐 아니라 AI 시스템에서도 관찰되는데:

- LLM이 출력에 대한 자기 평가나 수정 없이 첫 답변을 종결해버리는 경향
- 인간 학습자가 곧장 정답만 확인하고 반성적 사고를 생략해버리는 경향

이러한 메타인지적 태만은 성급하고 최적 이하의 결정을 초래하기 때문에, 이를 방지하려면 일부러라도 반추적(self-reflective) 과정을 삽입해야 한다고 지적된다.

**적용**: 제시된 시스템의 3요소 아키텍처는 이러한 문제에 대한 해결책이기도 하다. **Decision Projection Engine**은 아무 생각 없이 즉각적으로 답을 내는 대신, **과거 근거 탐색 → 가치 평가 → 설명 생성**의 추론 단계를 강제함으로써 AI/사용자 모두의 성급한 결정을 견제한다.

---

## 6. 핵심 설계 지침 및 구현 고려사항

### 6.1 자기 개념 및 핵심 가치의 보존 (가)

사용자의 과거 생각 파편을 가능한 한 왜곡 없이 원형 그대로 저장하고 제시함으로써, 개인이 가진 자기 개념과 핵심 가치를 일관되게 유지하도록 돕는다.

**이론적 근거**: Lecky의 자기일관성 이론[^1], Festinger의 인지부조화 이론[^3]

**구현 시 고려사항**:
- 원본 텍스트와 메타데이터(시간, 맥락, 감정) 분리 저장
- 원본 변형 금지 정책 (수정 시 버전 관리)
- 사용자의 명시적 삭제 요청 외에는 파편 영구 보존

---

### 6.2 가치-맥락 맵핑 및 모순 허용 (나)

사용자의 가치관은 단순한 일직선이 아니라 때로 상충하고 다면적인 네트워크로 존재할 수 있음을 인정해야 한다.

**이론적 근거**: Thagard의 인지-정서 지도[^9], Festinger의 인지부조화 이론[^3]

**구현 시 고려사항**:
- 그래프 데이터베이스 활용 (Neo4j, Amazon Neptune 등)
- 노드 속성: 가치명, 감정점수(-1~+1), 시간범위, 맥락태그
- 엣지 속성: 관계유형(지지/충돌/중립), 강도, 근거파편ID
- 모순 허용 정책: 충돌 엣지가 있어도 노드 삭제하지 않음

---

### 6.3 후회 예방형 의사결정 전략 (다)

시스템의 **Decision Projection Engine**은 단순히 이득을 극대화하는 추천을 하는 대신, 잠재적 후회를 최소화하는 방향으로 의사결정을 지원해야 한다.

**이론적 근거**: Loomes & Sugden의 후회 이론[^4], Coricelli의 신경과학 연구[^10]

**구현 시 고려사항 (수정됨)**:

> **문제점**: 미래 후회를 *사전에* 예측하려면 결과 데이터가 필요한데, 의사결정 시점에는 결과를 알 수 없다.

**해결 방안 - 사후 피드백 루프 설계**:

```
[의사결정 시점]
1. 과거 유사 결정의 후회 데이터 검색
2. 유사도 가중 후회 점수 계산
3. minimax regret 기반 옵션 랭킹

[의사결정 이후 - 피드백 수집]
4. 결과 발생 후 사용자에게 만족도/후회 설문
5. 해당 결정-결과-후회 데이터를 Fragment Store에 저장
6. 향후 유사 결정 시 참조 데이터로 활용
```

---

### 6.4 설명 가능성 및 자기성찰 유도 (라)

시스템은 최종 답을 일방적으로 주는 대신, 이해하기 쉬운 설명을 곁들여 사용자의 메타인지적 숙고를 이끌어내야 한다.

**이론적 근거**: Strack & Deutsch의 이중과정 이론[^15], 교육학 메타인지 연구[^16]

**LLM 환각(Hallucination) 통제 전략 (추가됨)**:

| 위험 | 대응 전략 |
|------|----------|
| 존재하지 않는 과거 파편 인용 | LLM에 검색된 파편만 컨텍스트로 제공, 생성 금지 프롬프트 |
| 과장된 확률/확신 표현 | 출력 후처리로 수치적 주장 검증 |
| 가치 판단 삽입 | "~일 수 있습니다", "~라고 말씀하셨습니다" 등 중립 어조 강제 |
| 사용자 조작 시도 | 설득/추천 금지, 정보 제시만 허용 |

**프롬프트 템플릿 예시**:
```
당신은 사용자의 과거 생각 파편을 기반으로 현재 결정과의 연관성을
*중립적으로 설명*하는 역할입니다.

규칙:
- 제공된 파편 외의 정보를 생성하지 마세요
- 특정 선택을 추천하거나 설득하지 마세요
- "~해야 합니다" 대신 "~라고 말씀하신 적 있습니다"를 사용하세요
- 확률이나 수치를 직접 계산하여 말하지 마세요
```

---

### 6.5 지속 학습 및 가치 갱신 (마)

인간의 가치관과 선호도는 시간에 따라 변화할 수 있으므로 시스템도 정적인 조력자가 아니라 동적인 동반자로 설계되어야 한다.

**이론적 근거**: Conway의 자서전적 기억 자기-메모리 시스템(SMS) 모델

**구현 시 고려사항**:
- 시간 가중치: 최신 파편에 더 높은 가중치
- 가치 변화 감지: 동일 주제에 대한 감정점수 변화 추적
- 명시적 가치 업데이트: 사용자가 "이제 X는 더 이상 중요하지 않아"라고 하면 그래프 반영
- 암묵적 가치 추론: 일정 기간 특정 가치 관련 파편이 없으면 중요도 감쇠

---

## 7. 참고문헌

[^1]: Lecky, P. (1945). *Self-consistency: A theory of personality*. Island Press.

[^2]: Psychology Fanatic. (2024). Understanding Self-Consistency Theory and Human Behavior. https://psychologyfanatic.com/self-consistency-theory/

[^3]: Festinger, L. (1957). *A Theory of Cognitive Dissonance*. Stanford University Press.

[^4]: Loomes, G., & Sugden, R. (1982). Regret Theory: An Alternative Theory of Rational Choice under Uncertainty. *The Economic Journal*, 92(368), 805-824.

[^5]: Bell, D. E. (1982). Regret in Decision Making under Uncertainty. *Operations Research*, 30(5), 961-981.

[^6]: Bower, G. H. (1981). Mood and memory. *American Psychologist*, 36(2), 129-148.

[^7]: Gilboa, I., & Schmeidler, D. (1995). Case-Based Decision Theory. *The Quarterly Journal of Economics*, 110(3), 605-639.

[^8]: Duncan, K. D., & Shohamy, D. (2016). Memory states influence value-based decisions. *Journal of Experimental Psychology: General*, 145(11), 1420-1426.

[^9]: Homer-Dixon, T., Milkoreit, M., Mock, S. J., Schröder, T., & Thagard, P. (2014). The Conceptual Structure of Social Disputes. *SAGE Open*, 4(1).

[^10]: Coricelli, G., Critchley, H. D., Joffily, M., O'Doherty, J. P., Sirigu, A., & Dolan, R. J. (2005). Regret and its avoidance: a neuroimaging study of choice behavior. *Nature Neuroscience*, 8, 1255-1262.

[^11]: Izuma, K., Matsumoto, M., Murayama, K., Samejima, K., Sadato, N., & Matsumoto, K. (2010). Neural correlates of cognitive dissonance and choice-induced preference change. *PNAS*, 107(51), 22014-22019.

[^12]: Rangel, A., Camerer, C., & Montague, P. R. (2008). A framework for studying the neurobiology of value-based decision making. *Nature Reviews Neuroscience*, 9, 545-556.

[^13]: Kahneman, D., & Tversky, A. (1979). Prospect Theory: An Analysis of Decision under Risk. *Econometrica*, 47(2), 263-291.

[^14]: Bleichrodt, H., & Wakker, P. P. (2015). Regret Theory: A Bold Alternative to the Alternatives. *The Economic Journal*, 125(583), 493-532.

[^15]: Strack, F., & Deutsch, R. (2004). Reflective and Impulsive Determinants of Social Behavior. *Personality and Social Psychology Review*, 8(3), 220-247.

[^16]: Carver, C. S., & Scheier, M. F. (1982). Control theory: A useful conceptual framework for personality–social, clinical, and health psychology. *Psychological Bulletin*, 92(1), 111-135.

[^17]: Fan, Y., et al. (2024). Beware of metacognitive laziness: Effects of generative artificial intelligence on learning motivation, processes, and performance. *British Journal of Educational Technology*. https://doi.org/10.1111/bjet.13544

---

## 부록: 시스템 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Interface                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Decision Projection Engine                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │ Similarity  │  │  Bayesian   │  │  LLM Explanation Gen    │ │
│  │  Search     │──│  Inference  │──│  (with hallucination    │ │
│  │             │  │  + Minimax  │  │   control)              │ │
│  │             │  │  Regret     │  │                         │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
         │                   │                    │
         ▼                   ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐
│ Thought Fragment│  │  Value Graph    │  │  Feedback Loop      │
│ Store           │  │  Engine         │  │  (Post-decision)    │
│                 │  │                 │  │                     │
│ - Raw text      │  │ - Semantic      │  │ - Satisfaction      │
│ - Timestamp     │  │   graph DB      │  │   survey            │
│ - Emotion score │  │ - Support/      │  │ - Regret scoring    │
│ - Context tags  │  │   conflict      │  │ - Learning update   │
│                 │  │   edges         │  │                     │
└─────────────────┘  └─────────────────┘  └─────────────────────┘
```

---

*이 문서는 Aletheia-Core 프로젝트의 이론적 기반을 정리한 것으로, 구현 단계에서 지속적으로 업데이트될 예정입니다.*
