var monthDays = [];
for (var day = 1; day <= 28; day++) {
	monthDays.push({value:day});
}

function isInsert() {
	var id = parseInt(getValue("accountFeeId"));
	return (isNaN(id) || id == 0);
}

function updateGeneratedTypes() {
	var params = $H();
	params.url = pathPrefix + "/listGeneratedTransferTypesForAccountFee";
	params['accountTypeId'] = getValue("accountTypeId");
	params['paymentDirection'] = getValue("accountFee(paymentDirection)");
	findTransferTypes(params, function(tts) {
		setOptions('generatedSelect', tts, false, false, 'name', 'id');
		setValue('generatedSelect', selectedGeneratedType);
	});
}

function updateFields() {
	
	var chargeMode = getValue("accountFee(chargeMode)");
	if (chargeMode == 'NEGATIVE_VOLUME_PERCENTAGE') {
		Element.hide('trDirection');
		setValue("paymentDirectionSelect", "TO_SYSTEM");
	} else {
		Element.show('trDirection');
	}
	Element[chargeMode == 'VOLUME_PERCENTAGE' ? 'show' : 'hide']('trTolerance');
	var paymentDirection = getValue("accountFee(paymentDirection)");
	Element[paymentDirection == 'TO_MEMBER' ? 'hide' : 'show']('trInvoiceMode');

	var isEnabled = $('enabledCheck').checked;
	var trEnabledSince = $('trEnabledSince');
	if (trEnabledSince) {
		var isScheduled = getValue("accountFee(runMode)") == "SCHEDULED";
		trEnabledSince[isEnabled && isScheduled ? 'show' : 'hide']();
	}

	var isVolume = chargeMode == 'VOLUME_PERCENTAGE' || chargeMode == 'NEGATIVE_VOLUME_PERCENTAGE'; 
	if (isVolume) {
		setValue("accountFee(runMode)", "SCHEDULED");
	}
	$('trRunMode')[isVolume ? 'hide' : 'show']();
	 
	var isScheduled = getValue("accountFee(runMode)") == "SCHEDULED";
	['trDay', 'trHour', 'trRecurrence'].each(function(tr) {
		Element[isScheduled ? 'show' : 'hide'](tr);
	});
	if (isScheduled) {
		var recurrence = getValue("accountFee(recurrence).field");
		switch (recurrence) {
			case "DAYS":
				setValue("daySelect", "");
				Element.hide("trDay");
				break;
			case "WEEKS":
				setOptions('daySelect', weekDays, false, false, 'name', 'value');
				setValue('daySelect', selectedDay);
				break;
			case "MONTHS":
				setOptions('daySelect', monthDays, false, false, 'value');
				setValue('daySelect', selectedDay);
				break;
		}
	} else {
		["recurrenceNumberText", "recurrenceFieldSelect", "daySelect", "hourSelect"].each(function(field) {
			var current = $(field);
			if (current) {
				setValue(current, "");
			}
		});
	}
}

Behaviour.register({
	'#backButton': function(button) {
		button.onclick = function() {
			self.location = pathPrefix + "/editAccountType?accountTypeId=" + getValue("accountTypeId");
		}
	},
	
	'form': function(form) {
		form.onsubmit = function() {
			if (requestValidation(form)) {
				if (!booleanValue(getValue("enabledCheck")) && disablingConfirmation != null) {
					return confirm(disablingConfirmation);
				} else {
					return true;
				}
			}
		}
	},
	
	'#enabledCheck': function(check) {
		check.onclick = updateFields;
	},
	
	'#descriptionText': function(textarea) {
		new SizeLimit(textarea, 1000);
	},
	
	'#modifyButton': function(button) {
		button.onclick = function() {
			enableFormFields.apply(button.form, ["chargeModeText", "runModeText", "recurrenceText"]);
			getObject('accountFee(name)').focus();
		}
	},
	
	'#paymentDirectionSelect': function(select) {
		select.onchange = function() {
			updateFields();
			updateGeneratedTypes();
		};
	},
	
	'#chargeModeSelect': function(select) {
		select.onchange = updateFields;
	},
	
	'#runModeSelect': function(select) {
		select.onchange = updateFields;
	},
	
	'#recurrenceFieldSelect': function(select) {
		select.onchange = updateFields;
	}
});

Event.observe(self, "load", function() {
	if (isInsert()) {
		updateGeneratedTypes();
	}
	updateFields();
	if (isInsert()) {
		enableFormForInsert();
	}
});
