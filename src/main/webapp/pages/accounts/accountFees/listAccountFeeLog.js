Behaviour.register({
	'a.cancelFee': function(a) {
		setPointer(a);
		a.style.textDecoration = "underline";
		a.onclick = function() {
			if (confirm(cancelConfirmation)) {
				self.location = pathPrefix + "/cancelRunningAccountFee?accountFeeLogId=" + a.getAttribute("accountFeeLogId");
			}
		}
	},
	
	'a.runFee': function(a) {
		setPointer(a);
		a.style.textDecoration = "underline";
		a.onclick = function() {
			if (confirm(runConfirmation)) {
				self.location = pathPrefix + "/runAccountFee?accountFeeId=" + a.getAttribute("accountFeeId");
			}
		}
	},

	'a.rechargeFee': function(a) {
		setPointer(a);
		a.style.textDecoration = "underline";
		a.onclick = function() {
			if (confirm(rechargeConfirmation)) {
				self.location = pathPrefix + "/rechargeAccountFee?accountFeeLogId=" + a.getAttribute("accountFeeLogId");
			}
		}
	}
});
//Refresh disabled 
//setTimeout("self.location.reload(true)", 30000);