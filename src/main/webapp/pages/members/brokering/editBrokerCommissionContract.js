Behaviour.register({
	'#backButton': function(button) {
		button.onclick = function() {
			var params = $H();
			if (byBroker) {
				params['memberId'] = getValue("memberId");
			}
			backToLastLocation(params);
		}
	},
	
	'form': function(form) {
		form.onsubmit = function() {
			return requestValidation(form);
		}
	},
	
	'#cancelButton': function(button) {
		button.onclick = function() {
			if (confirm(cancelConfirmation)) {
				var brokerCommissionContractId = getValue("brokerCommissionContractId");
				self.location = pathPrefix + "/cancelBrokerCommissionContract?brokerCommissionContractId=" + brokerCommissionContractId;
			}
		}
	},
	
	'#removeButton': function(button) {
		button.onclick = function() {
			if (confirm(removeConfirmation)) {
				var brokerCommissionContractId = getValue("brokerCommissionContractId");
				var memberId = getValue("memberId");
				self.location = pathPrefix + "/removeBrokerCommissionContract?brokerCommissionContractId=" + brokerCommissionContractId + "&memberId=" + memberId;
			}
		}
	},
	
	'#acceptButton': function(button) {
		button.onclick = function() {
			if (confirm(acceptConfirmation)) {
				var brokerCommissionContractId = getValue("brokerCommissionContractId");
				self.location = pathPrefix + "/acceptBrokerCommissionContract?brokerCommissionContractId=" + brokerCommissionContractId;
			}
		}
	},
	
	'#denyButton': function(button) {
		button.onclick = function() {
			if (confirm(denyConfirmation)) {
				var brokerCommissionContractId = getValue("brokerCommissionContractId");
				self.location = pathPrefix + "/denyBrokerCommissionContract?brokerCommissionContractId=" + brokerCommissionContractId;
			}
		}
	},
	
	'#modifyButton': function(button) {
		button.onclick = function() {
			enableFormFields.apply(button.form, ["broker", "member", "brokerCommission", "status", "removeButton"]);
		}
	}
	
});

Event.observe(self, "load", function() {
	if (isInsert) {
		enableFormForInsert();
	}
});
