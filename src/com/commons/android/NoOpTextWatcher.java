package com.commons.android;

import com.commons.android.geo.BasicAutocompleteHelper;

import android.text.Editable;
import android.text.TextWatcher;

public class NoOpTextWatcher implements TextWatcher {

  private BasicAutocompleteHelper autocompleteHelper;
  
  public NoOpTextWatcher( BasicAutocompleteHelper autocompleteHelper ) {
    this.autocompleteHelper = autocompleteHelper;
  }

  @Override 
  public void onTextChanged( CharSequence txt, int start, int before, int count ) {
    if( null != autocompleteHelper ) autocompleteHelper.checkTextWatcher();
  }
  
  @Override public void afterTextChanged( Editable arg0 ) {}

  @Override public void beforeTextChanged( CharSequence arg0, int arg1, int arg2, int arg3 ) {}

}
