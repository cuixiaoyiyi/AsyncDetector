package com.example.repeatstart;

import android.view.View;
import android.widget.TextView;

public class Test {
	
	private Tasker mTasker = null;
	
	private View mView = null;

	public Test(View view){
		mView = view;
	}
	
	public void newTasker(){
		mTasker = new Tasker((TextView) mView);
	}
	
	public void  startTask() {
		mTasker.execute("");
	}
	
	public void cancel(){
		if(mTasker!=null){
			mTasker.cancel(true);
		}
	}
}
