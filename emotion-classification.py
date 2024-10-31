from dotenv import load_dotenv
import os
from openai import OpenAI

import json
import pandas as pd
import pytchat

# .env 파일 로드
load_dotenv()

# OpenAI API 키 설정
client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY"),
)

# 감정 분류를 수행하는 함수
def classify_emotion_batch(user_inputs):

    input_text = "\n".join([f"댓글 {i+1}: {comment}" for i, comment in enumerate(user_inputs)])
    
    instruction = """유튜브 방송에 달리는 댓글들을 제공할 것입니다. 댓글들의 전체적인 감정을 분류하세요. 전체적인 분위기와 유튜브의 특성을 고려하세요.
    1. 문장이 나누어져 있더라도 전체 문맥의 의미를 파악하여 분류하세요.
    2. 반어법, 비꼼, 풍자는 실제 의도를 파악하여 분류하세요.
    3. 이모티콘이나 이모지는 문맥의 보조 수단으로만 사용하세요.
    다음 형식으로 응답해 주세요:
    {
        "emotion": "기쁨/재미/슬픔/분노/놀람/혐오/혼란"
    }

    응답 예시:
    {
        "emotion": "기쁨"
    }
    """

    # OpenAI API 호출
    response = client.chat.completions.create(
        messages=[
            {"role": "system", "content": instruction},
            {"role": "user", "content": f"다음 댓글들의 종합적인 감정을 분석해주세요: '{input_text}'"}
        ],
        model="gpt-4o-mini",
    )

    gpt_output = response.choices[0].message.content.strip()
    print(f'gpt_output: {gpt_output}')

    json_str = gpt_output
    if "```json" in gpt_output:
        json_str = gpt_output.split("```json")[1].split("```")[0].strip()
    elif "```" in gpt_output:
        json_str = gpt_output.split("```")[1].strip()

    result = json.loads(json_str)

    return result['emotion']

emotion_list = ["기쁨", "재미", "슬픔", "분노", "놀람", "혐오", "혼란"]

# CSV 파일 경로
file_path = './youtube_comments_sentiment.csv'

# 유튜브 채팅 가져오기 및 감정 분석 실행
video_id = '4i-4IEGFifY'  # test

# 데이터프레임의 컬럼 정의
empty_frame = pd.DataFrame(columns=['댓글 작성자', '댓글 내용', '댓글 작성 시간', '감정'])

# 10개의 댓글을 모아 일괄 분석
comment_batch = []

# pytchat 라이브러리로 유튜브 채팅 연결
chat = pytchat.create(video_id=video_id)
while chat.is_alive():
    try:
        data = chat.get()
        items = data.items
        for c in items:
            # 댓글 내용과 작성자 정보 출력
            print(f"{c.datetime} [{c.author.name}]- {c.message}")
            comment_batch.append({
                '댓글 작성자': c.author.name,
                '댓글 내용': c.message,
                '댓글 작성 시간': c.datetime
            })
            data.tick()

            # 20개씩 모아서 감정 분석 실행
            if len(comment_batch) >= 10:
                comments = [comment['댓글 내용'] for comment in comment_batch]
                emotion = classify_emotion_batch(comments)  # 전체 감정 결과 하나만 반환

                # 감정 결과를 데이터프레임에 저장
                for comment in comment_batch:
                    comment['감정'] = emotion # 동일한 감정 값 저장
                result = pd.DataFrame(comment_batch)
                result.to_csv(file_path, mode='a', header=False, index=False)
                
                print(f"감정 분석 결과: {emotion}\n")

                # comment_batch 비우기
                comment_batch = []

    except KeyboardInterrupt:
        print("종료됨")
        chat.terminate()
        break
    except Exception as e:
        print(f"오류 발생: {e}")
        chat.terminate()
        break
