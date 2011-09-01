Behaviour.register({
	'.optimize': function(element) {
		if (element.tagName.toUpperCase() == 'A') {
			setPointer(element);
		}
		element.onclick = function() {
			self.location = pathPrefix + "/optimizeIndexes?index=" + element.getAttribute("entitytype");
		}
	},

	'.rebuild': function(element) {
		if (element.tagName.toUpperCase() == 'A') {
			setPointer(element);
		}
		element.onclick = function() {
			if (confirm(rebuildConfirmation)) {
				self.location = pathPrefix + "/rebuildIndexes?index=" + element.getAttribute("entitytype");
			}
		}
	},

	'#setStateButton': function(button) {
		button.onclick = function() {
			var online = booleanValue(button.getAttribute("online"));
			if (!online && !confirm(setOfflineConfirmation)) {
				return;
			}
			self.location = pathPrefix + "/setSystemOnline?online=" + online;
		}
	}
});