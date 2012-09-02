package com.chocolatelabs.smsfix;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class SMSFixScreen2 extends Activity {

	Button finishButton;
	public HashMap<String, ArrayList<String>> contactHM = new HashMap<String, ArrayList<String>>();
	public HashMap<String, String> numberHM = new HashMap<String, String>();
	public String[] contactsArray, numbersArray, idArray;
	public Boolean[] listArray;
	public String label = "Google Voice";
	ArrayAdapter<String> contactsArrayAdapter, contactsSpinnerAdapter;
	public String contacts = "";
	public String numbers = "";

	ListView contactList;
	
	public void onCreate(Bundle savedInstanceState) {
		
      super.onCreate(savedInstanceState);
      setContentView(R.layout.screen2);
      finishButton = (Button) findViewById(R.id.finishButton);
      finishButton.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
        	  addContacts();
          }
      });
      getContacts();
      searchSMS();
      genList();
  }

	
public String listArray(Boolean[] list) {
	String output = "";
	for (int i = 0; i < list.length; i++) {
		output += list[i] + ", ";
	}
	return output.substring(0, output.length()-2);
}

public void getContacts() { //Currently only supports phones without duplicate contact names.
	
      Cursor cursor = getContentResolver().query(Phone.CONTENT_URI, null, null, null, null);
      if (cursor.getCount() > 0) {
    	  
      	while (cursor.moveToNext()) {
      		if (cursor.getInt(cursor.getColumnIndex(Phone.HAS_PHONE_NUMBER)) > 0) {
	      			
	      		String contactId = cursor.getString(cursor.getColumnIndex(Phone.RAW_CONTACT_ID));
	      		String contactName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
	      		String contactNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));

	
	      		ArrayList<String> nameList = new ArrayList<String>();
	      		
	      		//Add duplicate IDs
	      		if (contactHM.containsKey(contactName)) {
	      			nameList = contactHM.get(contactName);
	      		}
	      		
				nameList.add(contactId);
				
	       		contactHM.put(contactName, nameList);
	       		numberHM.put(contactId, contactNumber);
      		}
      	}
      }
      cursor.close();
  }

  public void searchSMS() {
	  
	  Uri smsUri = Uri.parse("content://sms/inbox");
      Cursor sms = getContentResolver().query(
    		  smsUri,
    		  null,
    		  null,
    		  null,
    		  null);
	  
      while (sms.moveToNext()) {
    	  
    	  String smsNumber = sms.getString(sms.getColumnIndex("address")); //If the column names are not in the phone's API then this app will fall apart. Getting column by the number changes throughout different phones and a second method would have to be introduced to detect the address and body based off the given content.
    	  String smsContent = sms.getString(sms.getColumnIndex("body"));
				  
    	  if (smsContent != null && smsContent.contains(" - ")) {
    		  
    		  String possibleContact = smsContent.substring(0, smsContent.indexOf(" - "));
    		  if (contactHM.containsKey(possibleContact)) {
    			  
    			  boolean added = false;
    			  
    			  Cursor phones = getContentResolver().query(
    					  Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
    					  Uri.encode(smsNumber)), 
    					  new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup.LABEL},
    					  null,
    					  null,
    					  null);
    			  
    			  while (phones.moveToNext() && !added) {
    				    //String contactName = phones.getString(phones.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
    				    //String contactLabel = phones.getString(phones.getColumnIndexOrThrow(PhoneLookup.LABEL));
    				    added = true;
    			  }
    	
    			  if (!added) {
    				  contacts += possibleContact + ",";
    				  numbers += smsNumber + ",";
    			  }
       		  }
    	  }
      }
  }
  
  public void genList() {
	  
	  contactsArray = contacts.split(",");
      numbersArray = numbers.split(",");
      idArray = new String[contactsArray.length];
      listArray = new Boolean[contactsArray.length];
      
	  if (contactsArray[0] != "" && numbersArray[0] != "") {
	      contactList = (ListView) findViewById(R.id.contactList);
	      contactList.setItemsCanFocus(false);
	      contactList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	      //contactList.setClickable(false);

    	  contactsArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, contactsArray);
    	  contactList.setAdapter(contactsArrayAdapter);
    	  
    	  //Go through and check all items
    	  for (int i = 0; i < contactList.getCount(); i++) {
    		  //If there is a duplicate contact, turn this list item into a button so that the user can choose the correct contact.
    		  if (contactHM.get(contactList.getItemAtPosition(i)).size() == 1) {
        		  contactList.setItemChecked(i, true);
        		  idArray[i] = contactHM.get(contactList.getItemAtPosition(i)).get(0);
        		  listArray[i] = true;
    		  } else {
        		  idArray[i] = null;
        		  listArray[i] = false;
    		  }
    		  
    		 
    	  }
	  
		  contactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
				//Don't prompt for the correct contact unless 1) It's going to be checked (also note that the checked event occurs before this event) and 2) It has more than one contact associated with the name.
				ArrayList<String> duplicatesList = contactHM.get(contactList.getItemAtPosition(position));
				if (contactList.isItemChecked(position)) {
					listArray[position] = true;
					if (duplicatesList.size() > 1) {
						launchContactSpinner(duplicatesList, position);
					}
				} else {
					listArray[position] = false;
				}
			}
		  });
	  } else { //skip straight to the finished message.
		  addContacts();
	  }
  }
  
  public void launchContactSpinner(final ArrayList<String> duplicatesList, final int position) {
	  ArrayList<String> namesList = new ArrayList<String>();
	  for (int i = 0; i < duplicatesList.size(); i++) {
		  namesList.add(numberHM.get(duplicatesList.get(i)));
	  }
	  contactsSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, namesList);
	  new AlertDialog.Builder(this)
	  .setTitle("Select an option.")
	  .setAdapter(contactsSpinnerAdapter, new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int which) {
			  idArray[position] = duplicatesList.get(which);
			  dialog.dismiss();
		  }
	  }).create().show();
  }
  
  public void addContacts() {
	  
	  if (contactsArray[0] != "" && numbersArray[0] != "") {
		  String contactName, contactNumber, contactId;
		  for (int i = 0; i < contactsArray.length; i++) {
			  contactName = contactsArray[i];
			  contactNumber = numbersArray[i];
			  contactId = idArray[i];
			  if (listArray[i]) {
				  addContactInfo(contactName, contactId, contactNumber, label);
			  }
		  }
	  }
	  
	  new AlertDialog.Builder(this)
	  .setTitle("Done!")
	  .setMessage("Your contacts have been updated.")
	  .setNeutralButton("OK", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int which) {
			  dialog.dismiss();
			  launchScreen1();
		  }
	  }).create().show();
  }
  
  public void addContactInfo(String contactName, String contactId, String contactNumber, String contactLabel) {
	  ContentValues cv = new ContentValues();
	  cv.put(Data.RAW_CONTACT_ID, contactId);
	  cv.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
	  cv.put(Phone.NUMBER, contactNumber);
	  cv.put(Phone.TYPE, Phone.TYPE_CUSTOM);
	  cv.put(Phone.LABEL, contactLabel);
	  getContentResolver().insert(ContactsContract.Data.CONTENT_URI, cv);
  }
  
  protected void launchScreen1() {
      Intent i = new Intent(this, SMSFixScreen1.class);
      startActivity(i);
  }
}