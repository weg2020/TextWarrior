/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.applicationcontrol.*;
import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.i18n.ResourceBundle;

public class TextWarriorApplication extends UiApplication
implements TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	
	public TextWarriorApplication() { 
		TextWarriorController c = new TextWarriorController();
        checkPermissions();
        pushScreen(c.getMainScreen()); 
    } 
 
    public static void main(String[] args) {
        TextWarriorApplication app = new TextWarriorApplication(); 
        app.enterEventDispatcher(); 
    }
    
    public void checkPermissions(){
    	// Check if keystroke injection is permitted
        ApplicationPermissionsManager apm = ApplicationPermissionsManager.getInstance();
        ApplicationPermissions original = apm.getApplicationPermissions();

        //!! PERMISSION_EVENT_INJECTOR deprecated;
        //!! use PERMISSION_INPUT_SIMULATION for API 4.6 and above
        if(original.getPermission(ApplicationPermissions.PERMISSION_EVENT_INJECTOR) ==
        	ApplicationPermissions.VALUE_ALLOW){
            return;
        }

        // Set up and attach a reason provider
        ReasonProvider rp = new ReasonProvider(){
        	public String getMessage(int permissionID){
        		return _appStrings.getString(PROMPT_SET_KEYSTROKE_INJ);
        	}
        };
        apm.addReasonProvider(ApplicationDescriptor.currentApplicationDescriptor(), rp);
        
        // Create a permission request for keystoke injection.
        ApplicationPermissions permRequest = new ApplicationPermissions();
        permRequest.addPermission(ApplicationPermissions.PERMISSION_EVENT_INJECTOR);
        apm.invokePermissionsRequest(permRequest);
    }
}
