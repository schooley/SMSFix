package com.chocolatelabs.smsfix;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.widget.TextView;

public class SMSFixActivity extends Activity {

	public HashMap<String, String> contactHM = new HashMap<String, String>();
	public ArrayList<String> contacts = new ArrayList<String>();
	public String disp;
	public String label = "Google Voice";
	
	public void onCreate(Bundle savedInstanceState) {

      super.onCreate(savedInstanceState);
      
      TextView tv = new TextView(this);
      disp = "Activity Launched.\n";
      populateContacts();
      searchSMS();
      addContacts();
      tv.setText(disp);
      setContentView(tv);
  }

  public void populateContacts() { //Currently only supports phones without duplicate contact names.
	  disp += "Populating contacts...\n";
      Cursor cursor = getContentResolver().query(
      ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
      if (cursor.getCount() > 0) {
      	while (cursor.moveToNext()) {
      		String contactId = cursor.getString(cursor.getColumnIndex(RawContacts._ID));
      		String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
       		if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) { //Contact must have a phone number or else we get email and all that crap.
      			contactHM.put(contactName, contactId);
      			//disp += name + " : " + id + "\n";
      		}
      	}
      }
      disp += "Contacts populated. \n";
      cursor.close();
  }
  public void searchSMS() {
	  disp += "Searching for possible contacts...\n";
	  Uri smsUri = Uri.parse("content://sms/inbox");
      Cursor sms = getContentResolver().query(
    		  smsUri,
    		  null,
    		  null,
    		  null,
    		  null);
      while (sms.moveToNext()) {
    	  
    	  String smsNumber = sms.getString(sms.getColumnIndex("address"));
    	  String smsContent = sms.getString(sms.getColumnIndex("body"));
    	  if (smsContent != null && smsContent.contains(" - ")) {
    		  
    		  String possibleContact = smsContent.substring(0, smsContent.indexOf(" - "));
    		  disp += "Possible contact found...\n";
    		  if (contactHM.containsKey(possibleContact)) {
    			  
    			  disp += "Contact " + possibleContact + " confirmed. Adding...\n";
    			  boolean added = false;
    			  
    			  Cursor phones = getContentResolver().query(
    					  Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
    					  Uri.encode(smsNumber)), 
    					  new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup.LABEL},
    					  null,
    					  null,
    					  null);
    			  
    			  while (phones.moveToNext() && !added) {
    				    String contactName = phones.getString(phones.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
    				    String contactLabel = phones.getString(phones.getColumnIndexOrThrow(PhoneLookup.LABEL));
    				    disp += "Contact " + contactName + " has already been added under " + contactLabel + ".\n";
    				    added = true;
    			  }
    			  
    			  if (!added) {
    				  contacts.add(possibleContact);
    				  contacts.add(smsNumber);
    			  }
    		  }
    	  }
      }
      disp += "Done searching.\n";
  }
  public void addContacts() {
	  String contactName, contactNumber;
	  for (int i = 0; i < contacts.size(); i++) {
		  contactName = contacts.get(i);
		  i++;
		  contactNumber = contacts.get(i);
		  disp += contactName + ": " + contactNumber + "\n";
		  addContactInfo(contactName, contactHM.get(contactName), contactNumber, label);
	  }
  }
  
  public void addContactInfo(String contactName, String contactId, String contactNumber, String contactLabel) {
	  ContentValues cv = new ContentValues();
	  cv.put(Data.RAW_CONTACT_ID, contactId);
	  cv.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
	  cv.put(Phone.NUMBER, contactNumber);
	  cv.put(Phone.TYPE, Phone.TYPE_CUSTOM);
	  cv.put(Phone.LABEL, contactLabel);
	  getContentResolver().insert(Data.CONTENT_URI, cv);
	  disp += contactName + " added!\n";
  }
}