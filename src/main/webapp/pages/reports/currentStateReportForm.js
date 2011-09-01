Behaviour.register({
	
	'#selectAllButton': function(button) {
		button.onclick = function() {
			$$('.checkbox').each(function(check) {
				check.checked = true;
			});
		}
	},
	
	'#selectNoneButton': function(button) {
		button.onclick = function() {
			$$('.checkbox').each(function(check) {
				check.checked = false;
			});
		}
	}
	
});