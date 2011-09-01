var fromSystemGeneratedTypes = [];

function isInsert() {
	var id = parseInt(getValue("transactionFeeId"));
	return (isNaN(id) || id == 0);
}

function updateWhen() {
	var isAlways = getValue("whenSelect") == "ALWAYS";
	if (isAlways) {
		setValue('transactionFee(count)', null);
	} 
	Element[isAlways ? "hide" : "show"]("countText");
}

function updateFields() {
	updateWhen();
	updateBrokerGroups();
}

function updateFromGroups() {
	var check = $('fromAllCheck');
	if (!check) return;
	var tr = $('trFromGroups');
	if (check.checked) {
		tr.hide();
	} else {
		tr.show();
		fromGroupsSelect.updateValues();
	}
}

function updateToGroups() {
	var check = $('toAllCheck');
	if (!check) return;
	var tr = $('trToGroups');
	if (check.checked) {
		tr.hide();
	} else {
		tr.show();
		toGroupsSelect.updateValues();
	}
}

function updateBrokerGroups() {
	var check = $('brokerCheck');
	if (!check) return;
	var trBrokerGroups = $('trBrokerGroups');
	if (check.checked) { 
		trBrokerGroups.hide();
	} else {
		trBrokerGroups.show();
		brokerGroupsSelect.updateValues();
	}
}

function updateGeneratedTypes() {
	var anyAccount = $("allowAnyAccount");
	var selectedGeneratedType = getValue("transactionFee(generatedTransferType)");
	var params = $H();
	params.url = pathPrefix + "/listGeneratedTypesForTransactionFee";
	params['nature'] = "BROKER";
	if (anyAccount) {
		params['allowAnyAccount'] = anyAccount ? anyAccount.checked : true;
	}
	params['transferTypeId'] = getValue("transferTypeId");
	params['payer'] = getValue("transactionFee(payer)");
	params['whichBroker'] = getValue("transactionFee(whichBroker)");
	params['direction'] = true;
	if (isFromSystem) {
		params['fromNature'] = 'SYSTEM';
	}
	findTransferTypes(params, function(tts) {
		fromSystemGeneratedTypes = tts.select( 
			function(tt) {
				return booleanValue(tt.fromSystem);
			}
		).pluck("id");
		setOptions('generatedSelect', tts, false, false, 'name', 'id');
		setValue('generatedSelect', selectedGeneratedType);
		generatedTypeUpdated();
	});
}


function generatedTypeUpdated() {
	var generatedTransferTypeId = getValue("transactionFee(generatedTransferType)");
	var isFromSystem = fromSystemGeneratedTypes.include(generatedTransferTypeId);
	if (!isFromSystem) { // SOURCE_BROKER or DESTINATION_BROKER
		// Broker commission paid by member
		$$(".trMaxAmount").each(function(element) {
			Element['show'](element);
		});
	} else { 
		// Broker commission paid by system
		$$(".trMaxAmount").each(function(element) {
			Element['hide'](element);
		});
		setValue('transactionFee(maxFixedValue)', null);
		setValue('transactionFee(maxPercentageValue)', null);
	}
}

Behaviour.register({
	'#backButton': function(button) {
		button.onclick = function() {
			self.location = pathPrefix + "/editTransferType?accountTypeId=" + getValue("accountTypeId") + "&transferTypeId=" + getValue("transferTypeId");
		}
	},
	
	'form': function(form) {
		form.onsubmit = function() {
			return requestValidation(form);
		}
	},
		
	'#descriptionText': function(textarea) {
		new SizeLimit(textarea, 1000);
	},
	
	'#modifyButton': function(button) {
		button.onclick = function() {
			enableFormFields.apply(button.form, ['payerText', 'transferTypeName', 'whichBrokerText', 'generatedTransferTypeName']);
			getObject('transactionFee(name)').focus();
		}
	},
	
	'#payerSelect': function(select) {
		select.onchange = updateGeneratedTypes;
	},
	
	'#whichBrokerSelect': function(select) {
		select.onchange = updateGeneratedTypes;
	},
	
	'#allowAnyAccount': function(checkbox) {
		checkbox.onchange = updateGeneratedTypes;
	},
	
	'#generatedSelect': function(select) {
		select.onchange = function() {
			generatedTypeUpdated();
		};
	},
		
	'#whenSelect': function(select) {
		select.onchange = updateWhen;
	},
	
	'#fromAllCheck': function(checkbox) {
		checkbox.onclick = updateFromGroups;
	},
	
	'#toAllCheck': function(checkbox) {
		checkbox.onclick = updateToGroups;
	},
	
	'#brokerCheck': function(checkbox) {
		checkbox.onclick = updateBrokerGroups;
	}
});

Event.observe(self, "load", function() {
	if (editableGeneratedTT) {
		updateGeneratedTypes();
	} else {
		generatedTypeUpdated();
	}
	updateFields();
	updateFromGroups();
	updateToGroups();
	updateBrokerGroups();
	if (isInsert()) {
		enableFormForInsert();
	}
});
