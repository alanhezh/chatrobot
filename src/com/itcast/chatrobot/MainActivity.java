package com.itcast.chatrobot;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.itcast.chatrobot.VoiceBean.WS;

public class MainActivity extends Activity {

	private StringBuffer mBuffer;
	private ListView lvList;

	private ArrayList<TalkBean> mList = new ArrayList<TalkBean>();
	private MyAdapter mAdapter;

	private String[] mAnswers = new String[] { "约吗?", "这张怎么样?", "漂不漂亮呀?",
			"一晚上500块呀!" };

	private int[] mPics = new int[] { R.drawable.p1, R.drawable.p2,
			R.drawable.p3, R.drawable.p4 };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//将“12345678”替换成您申请的 APPID，申请地址：http://www.xfyun.cn
		// 请勿在“=”与 appid 之间添加任务空字符或者转义符
		SpeechUtility.createUtility(this, SpeechConstant.APPID + "=568d036a");

		lvList = (ListView) findViewById(R.id.lv_list);
		mAdapter = new MyAdapter();
		lvList.setAdapter(mAdapter);
	}

	/**
	 * 开始语音识别
	 * @param view
	 */
	public void startListen(View view) {
		//1.创建RecognizerDialog对象
		RecognizerDialog mDialog = new RecognizerDialog(this, null);
		//2.设置accent、language等参数
		mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
		//若要将UI控件用于语义理解，必须添加以下参数设置，设置之后onResult回调返回将是语义理解
		//结果
		// mDialog.setParameter("asr_sch", "1");
		// mDialog.setParameter("nlp_version", "2.0");

		mBuffer = new StringBuffer();

		//3.设置回调接口
		mDialog.setListener(new RecognizerDialogListener() {

			//听写结果回调接口(返回Json格式结果，用户可参见附录13.1)；
			//一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
			//关于解析Json的代码可参见Demo中JsonParser类；
			//isLast等于true时会话结束。
			@Override
			public void onResult(RecognizerResult results, boolean isLast) {
				String result = results.getResultString();
				//System.out.println("识别结果:" + result);
				String parseData = parseData(result);
				//System.out.println("解析结果:" + parseData);
				//System.out.println("isLast:" + isLast);
				mBuffer.append(parseData);
				if (isLast) {
					//会话结束
					String askContent = mBuffer.toString();
					System.out.println("最终结果:" + askContent);

					//初始化提问对象
					TalkBean askBean = new TalkBean(askContent, -1, true);
					mList.add(askBean);

					//初始化回答对象
					String answer = "没听清";
					int imageId = -1;
					if (askContent.contains("你好")) {
						answer = "你好呀!";
					} else if (askContent.contains("你是谁")) {
						answer = "我是你的小助手!";
					} else if (askContent.contains("美女")) {
						//随机回答
						int i = (int) (Math.random() * mAnswers.length);//0,1,2,3
						answer = mAnswers[i];

						int j = (int) (Math.random() * mPics.length);//0,1,2,3
						imageId = mPics[j];
					} else if (askContent.contains("天王盖地虎")) {
						answer = "小鸡炖蘑菇";
						imageId = R.drawable.m;
					}

					TalkBean answerBean = new TalkBean(answer, imageId, false);
					mList.add(answerBean);

					//刷新listview
					mAdapter.notifyDataSetChanged();

					//让listview自动显示最后一个条目
					lvList.setSelection(mList.size() - 1);

					startSpeak(answer);
				}
			}

			@Override
			public void onError(SpeechError arg0) {

			}

		});
		//4.显示dialog，接收语音输入
		mDialog.show();

	}

	/**
	 * 语音合成
	 * @param view
	 */
	public void startSpeak(String content) {
		//1.创建 SpeechSynthesizer 对象, 第二个参数：本地合成时传 InitListener
		SpeechSynthesizer mTts = SpeechSynthesizer
				.createSynthesizer(this, null);
		//2.合成参数设置，详见《科大讯飞MSC API手册(Android)》SpeechSynthesizer 类
		//设置发音人（更多在线发音人，用户可参见 附录12.2
		mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan"); //设置发音人
		mTts.setParameter(SpeechConstant.SPEED, "50");//设置语速
		mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
		//设置合成音频保存位置（可自定义保存位置），保存在“./sdcard/iflytek.pcm”
		//保存在 SD 卡需要在 AndroidManifest.xml 添加写 SD 卡权限
		//仅支持保存为 pcm 和 wav 格式，如果不需要保存合成音频，注释该行代码
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, "./sdcard/iflytek.pcm");
		//3.开始合成
		mTts.startSpeaking(content, null);
	}

	/**
	 * 解析json
	 */
	protected String parseData(String json) {
		//Gson google
		Gson gson = new Gson();
		VoiceBean voiceBean = gson.fromJson(json, VoiceBean.class);

		StringBuffer sb = new StringBuffer();

		ArrayList<WS> ws = voiceBean.ws;
		for (WS w : ws) {
			String word = w.cw.get(0).w;
			sb.append(word);
		}

		return sb.toString();
	}

	class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public TalkBean getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = View.inflate(getApplicationContext(),
						R.layout.list_item, null);

				holder = new ViewHolder();
				holder.tvAsk = (TextView) convertView.findViewById(R.id.tv_ask);
				holder.tvAnswer = (TextView) convertView
						.findViewById(R.id.tv_answer);
				holder.ivPic = (ImageView) convertView
						.findViewById(R.id.iv_pic);
				holder.llAnswer = (LinearLayout) convertView
						.findViewById(R.id.ll_answer);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			TalkBean item = getItem(position);

			if (item.isAsk) {
				//提问
				holder.tvAsk.setVisibility(View.VISIBLE);
				holder.llAnswer.setVisibility(View.GONE);

				holder.tvAsk.setText(item.content);
			} else {
				//回答
				holder.tvAsk.setVisibility(View.GONE);
				holder.llAnswer.setVisibility(View.VISIBLE);

				holder.tvAnswer.setText(item.content);

				//图片
				if (item.imageId > 0) {
					holder.ivPic.setVisibility(View.VISIBLE);
					holder.ivPic.setImageResource(item.imageId);
				} else {
					holder.ivPic.setVisibility(View.GONE);
				}
			}

			return convertView;
		}

	}

	static class ViewHolder {
		public TextView tvAsk;

		public TextView tvAnswer;
		public ImageView ivPic;
		public LinearLayout llAnswer;
	}

}
