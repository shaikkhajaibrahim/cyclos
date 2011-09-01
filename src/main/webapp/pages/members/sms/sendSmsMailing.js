
var hideShowTarget = function(isToGroup) {
	$$('tr.toHideMember').each(function(tr) {
   		tr[isToGroup ? 'hide' : 'show']();
   	});
	$$('tr.toHideGroups').each(function(tr) {
   		tr[!isToGroup ? 'hide' : 'show']();
   	});
	
	if (isToGroup) {
		setValue("memberId", null);
		setValue("memberUsername", null);
		setValue("memberName", null);

		groupsMdd.render();
		try {
			$('trType').show();
			$('trFree').hide();
		} catch (e) {}
	} else {
		groupsMdd.values.each(function(value) {
		    value.selected = false;
		});	
		try {
			$('trType').hide();
			$('trFree').show();
		} catch (e) {}
	}
}

Behaviour.register({
	'form': function(form) {
		form.onsubmit = function() {
			return requestValidation(form);
		}
	},
	
	'#backButton': function(button) {
		button.onclick = function() {
			history.back();
		}
	},
	
	'#toMember': function(radio) {
		radio.onclick = function() {
			hideShowTarget(false);
		}
	},

	'#toGroup': function(radio) {
		radio.onclick = function() {
			hideShowTarget(true);
		}
	},

	'#text': function(text) {
		new SizeLimit(text, 160);
	},
	
	'#memberUsername': function(input) {
		var div = $('membersByUsername');
		prepareForMemberAutocomplete(input, div, {paramName:"username", enabled:true, brokered:true}, 'memberId', 'memberUsername', 'memberName', 'text');
	},
	
	'#memberName': function(input) {
		var div = $('membersByName');
		prepareForMemberAutocomplete(input, div, {paramName:"name", enabled:true, brokered:true}, 'memberId', 'memberUsername', 'memberName', 'text');
	}	
});
