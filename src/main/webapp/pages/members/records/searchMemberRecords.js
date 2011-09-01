Behaviour.register({
	
	'#typeSelect': function(select) {
		select.onchange = function() {
			select.form.submit();
		}
	},
	
	'#newButton': function(button) {
		button.onclick = function() {
			self.location = pathPrefix + "/editMemberRecord?elementId=" + getValue("elementId") + "&typeId=" + getValue("typeId");
		}
	},
	
	'#backButton': function(button) {
		button.onclick = function() {
			navigateToProfile(getValue("elementId"), elementNature);
		}
	},
	
	'#memberUsername': function(input) {
		var div = $('membersByUsername');
		prepareForMemberAutocomplete(input, div, {paramName:"username"}, 'queryElementId', 'memberUsername', 'memberName');
	},
	
	'#memberName': function(input) {
		var div = $('membersByName');
		prepareForMemberAutocomplete(input, div, {paramName:"name"}, 'queryElementId', 'memberUsername', 'memberName');
	},
	
	'img.memberRecordDetails': function(img) {
		setPointer(img);
		img.onclick = function() {
			self.location = pathPrefix + "/editMemberRecord?memberRecordId=" + img.getAttribute("memberRecordId") + "&global=" + getValue("global") ;
		}
	},

	'img.remove': function(img) {
		setPointer(img);
		img.onclick = function() {
			if (confirm(removeConfirmation)) {
				self.location = pathPrefix + "/removeMemberRecord?memberRecordId=" + img.getAttribute("memberRecordId");
			}
		}
	}
	
});


Event.observe(self, "load", function() {
	setFocus("keywords")
});