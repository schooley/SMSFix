package com.chocolatelabs.smsfix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SMSFixScreen1 extends Activity {
	
	Button startButton;
	
	public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.screen1);
      startButton = (Button) findViewById(R.id.startButton);
      startButton.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
        	  launchScreen2();
          }
      });
  }

  protected void launchScreen2() {
      Intent i = new Intent(this, SMSFixScreen2.class);
      startActivity(i);
  }
}