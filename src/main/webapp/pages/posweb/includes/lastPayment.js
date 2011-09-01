Behaviour.register({
	'#printLastReceipt': function(a) {
		setPointer(a);
		a.onclick = function() {
			var lastId = a.getAttribute("paymentId");
			var scheduled = booleanValue(a.getAttribute("isScheduled"));
			var url;
			if (scheduled) {
				url = context + "/do/printScheduledPayment?print=true&paymentId=" + lastId;
			} else {
				url = context + "/do/printTransaction?print=true&transferId=" + lastId;
			}
			printResults(null, url, 500, 300);
		}
	},
	
	'#closePrint': function(button) {
		button.onclick = function() {
			$('printDiv').hide();
			$('formTable').show();
			initFocus();
		}
	}
});