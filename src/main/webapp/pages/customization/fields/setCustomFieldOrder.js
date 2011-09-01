Behaviour.register({
	'#backButton': function(button) {
		button.onclick = function() {
			var params = $H();
			switch (getValue('nature')) {
				case 'MEMBER_RECORD':
					params['memberRecordTypeId'] = getValue('memberRecordTypeId');
					break;
				case 'PAYMENT':
					params['transferTypeId'] = getValue('transferTypeId');
					params['accountTypeId'] = getValue('accountTypeId');
					break;
			}
			backToLastLocation(params);
		}
	},
	
	'.draggableList': function(list) {
		Sortable.create(list);
	}
});