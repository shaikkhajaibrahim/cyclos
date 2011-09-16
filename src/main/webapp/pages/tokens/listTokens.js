Behaviour.register({

	'img.senderTokenRedemption': function(img) {
		setPointer(img);
		img.onclick = function() {
			self.location = pathPrefix + "/senderTokenRedemption?token(transactionId)=" + escape(img.getAttribute("transactionId"));
		}
	},

	'#newButton': function(button) {
		button.onclick = function() {
			self.location = pathPrefix + "/generateToken"
		}
	}


});
