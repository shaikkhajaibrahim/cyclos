Behaviour.register({

	'img.senderTokenRedemption': function(img) {
		setPointer(img);
		img.onclick = function() {
			self.location = pathPrefix + "/senderTokenRedemption?token(tokenId)=" + img.getAttribute("tokenId");
		}
	},

	'#newButton': function(button) {
		button.onclick = function() {
			self.location = pathPrefix + "/generateToken"
		}
	}


});
