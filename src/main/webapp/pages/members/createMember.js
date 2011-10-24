var registrationWarningTimeout;
var registrationTimeout;
var timeoutInMilis;
var secondsToTimeout;


function setPostAction(action) {
	var postAction = this.elements["postAction"];
	if (postAction == null) {
		//Create the hidden if not found
		postAction = document.createElement("input");
		postAction.setAttribute("type", "hidden");
		postAction.setAttribute("name", "postAction");
		this.appendChild(postAction);
	}
	postAction.value = action;
}

function newCaptcha() {
	var image = $('captchaImage');
	if (image) {
		image.src = context + "/captcha?random=" + new Date().getTime() + "_" + (Math.random() * 9999999999);
		image.show();
		setValue('captcha', '');
	}
}

function registrationTimeoutHandler() {
    window.location = "login";
}

function adjustTimeout() {
   if (document.getElementById("secondsToTimeout")) {
    document.getElementById("secondsToTimeout").innerHTML = secondsToTimeout;
    secondsToTimeout--;
    setTimeout("adjustTimeout()", 1000);
   }
}

function showTimeoutWarning() {
    secondsToTimeout = timeoutInMilis /2000;
    Element.show(document.getElementById("logoutWarning"));
    adjustTimeout();
}



function initTimeouts(timeout) {
   timeoutInMilis = timeout;
   setTimeoutActions();
   document.onkeypress = cancelTimeouts;
   document.onclick = cancelTimeouts;

}

function cancelTimeouts() {
    if (registrationWarningTimeout) {
        clearTimeout(registrationWarningTimeout);
    }
    if (registrationTimeout) {
        clearTimeout(registrationTimeout);
    }
    Element.hide(document.getElementById("logoutWarning"));
    setTimeoutActions();
}

function setTimeoutActions() {
    registrationWarningTimeout = setTimeout("showTimeoutWarning()", timeoutInMilis/2);
    registrationTimeout = setTimeout("registrationTimeoutHandler()", timeoutInMilis);
}


Behaviour.register({
	'#backButton': function(button) {
		button.onclick = function() {
			history.back();
		}
	},
	
	'#assignBrokerCheck': function(check) {
		check.onclick = function() {
			var isChecked = check.checked;
			$$('tr.trBroker').each(function(tr) {
				Element[isChecked ? 'show' : 'hide'](tr);
			});
			if (isChecked) {
				$('brokerUsername').focus();
			} else {
				setValue('newBrokerId', '');
				setValue('brokerUsername', '');
				setValue('brokerName', '');
			}
		}
	},

    '#registrationAgreementCheck': function(check) {
        disableField($('saveButton'));
        check.onclick = function() {
            if (check.checked) {
                enableField($('saveButton'))
            } else {
                disableField($('saveButton'))
            }
        }
    },

	'#assignPasswordCheck': function(check) {
		check.onclick = function() {
			var isChecked = check.checked;
			$$('tr.trPassword').each(function(tr) {
				Element[isChecked ? 'show' : 'hide'](tr);
			});
			if (isChecked) {
				$('_password').focus();
			} else {
				setValue('_password', '');
				setValue('_confirmPassword', '');
			}
		}
	},

	'#brokerUsername': function(input) {
		var div = $('brokersByUsername');
		prepareForMemberAutocomplete(input, div, {paramName:"username", brokers:true}, 'newBrokerId', 'brokerUsername', 'brokerName', 'comments');
	},

	'#brokerName': function(input) {
		var div = $('brokersByName');
		prepareForMemberAutocomplete(input, div, {paramName:"name", brokers:true}, 'newBrokerId', 'brokerUsername', 'brokerName', 'comments');
	},
	
	'form': function(form) {
		form.onsubmit = function() {
			var check = $('registrationAgreementCheck');
			if (check && ! check.checked) {
				alert(registrationAgreementNotCheckedMessage);
				return false;
			}
			return requestValidation(form);
		}
	},
	
	'input': function(checkbox) {
		var prefix = "chk_hidden_";
		if (checkbox.id.indexOf(prefix) >= 0) {
			checkbox.onclick = function() {
				$('hidden_' + this.id.substring(prefix.length)).value = this.checked;
			}.bindAsEventListener(checkbox);
		}
	},
	
	'#saveAndNewButton': function(button) {
		button.onclick = setPostAction.bind(button.form, 'new');
	},
	
	'#saveAndOpenProfileButton': function(button) {
		button.onclick = setPostAction.bind(button.form, 'openProfile');
	},
	
	'#printAgreement': function(a) {
		setPointer(a);
		a.onclick = function() {
			var win = window.open("", "_blank");
			win.title = self.title;
			win.document.open();
			win.document.write("<html><head><title>" + agreementPrintTitle + "</title></head><body><div style='font-weight:bold;font-size:larger'>" + agreementPrintTitle + "</div><br>" + $('registrationAgreement').innerHTML + "</body></html>");
			win.document.close();
			(function() {
				win.print()
			}).delay(1);
		}
	},
	
	'#newCaptcha': function(a) {
		setPointer(a);
		a.onclick = function() {
			newCaptcha();
			setFocus('captcha');
		}
	},
    '.InputBoxEnabled': function(input) {
        input.onmouseover = show;
        input.onmouseout = hide;
   	},
    'select': function(input) {
        input.onmouseover = show;
        input.onmouseout = hide;
   	}
});

function show() {
    var that = this;
    Element.extend(that);
    if (that.next('.msg')) {
        that.next('.msg').style.display = 'inline';
    }
}
function hide() {
    var that = this;
    Element.extend(that);
    if (that.next('.msg')) {
        that.next('.msg').style.display = 'none';
    }
}

Event.observe(self, "load", function() {
	(getObject("member(user).username") || getObject("member(name)")).focus();
	var assignBroker = $('assignBrokerCheck');
	if (assignBroker) {
		assignBroker.checked = false;
		assignBroker.onclick();
	}
	var assignPassword = $('assignPasswordCheck');
	if (assignPassword) {
		assignPassword.checked = false;
		assignPassword.onclick();
	}
	if (isPublic) {
		newCaptcha();
	}
	var pwd = getObject('member(user).password');
	if (pwd) {
		pwd.value = '';
		setValue('confirmPassword', '');
	}

})

