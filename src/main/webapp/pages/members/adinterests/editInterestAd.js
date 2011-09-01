Behaviour.register({
	'#backButton': function(button) {
		button.onclick = function() {
			var params = $H();
			params['memberId'] = getValue("ad(owner)");
			backToLastLocation(params);
		}
	},
	
	'#modifyButton': function(button) {
		button.onclick = function() {
			enableFormFields.apply(button.form, ['pictureFile', 'captionText']);
			getObject('ad(title)').focus();
		}
	},
		
	'#descriptionText': function(textarea) {
		new SizeLimit(textarea, 2048);
	},
	
	'#notExpirableCheck': function(check) {
		check.onclick = function() {
			var isChecked = check.checked;
			hidePublicationPeriod(isChecked, false);
		}
	},
	
	'#addPictureCheck': function(check) {
		check.onclick = function() {
			var isChecked = check.checked;
			showTrUploadPicture(isChecked);
			if (isChecked) {
				$('captionText').focus();
				enableField($('pictureFile'));
				enableField($('captionText'));
			} else {
				disableField($('pictureFile'));
				disableField($('captionText'));
			}
		}
	},
	
	'form': function(form) {
		form.onsubmit = function() {
			return requestValidation(form);
		}
	}
	
});

Event.observe(self, "load", function() {
	var hidePeriod = $F('notExpirableCheck');
	hidePublicationPeriod(hidePeriod, true);
	
	var check = $('addPictureCheck');
	if (check) {
		check.checked = false;
	}
	if (typeof(maxImages) == "boolean") {
		showTrPictureCheck(!maxImages);
	}
	$$('.imageContainerDiv').min().container.onRemove=function() {
		showTrPictureCheck(true);
		if (check) {
			showTrUploadPicture(check.checked);
		}
	}
	if (isEmpty(getValue("ad(id)"))) {
		enableFormForInsert();
	}
})

function showTrPictureCheck(showTrCheck) {
	Element[showTrCheck ? 'show' : 'hide']("trPictureCheck");
	Element[showTrCheck ? 'hide' : 'show']("trMaxPictures");
}

function showTrUploadPicture(showTrUp) {
	$$('tr.trPicture').each(function(tr) {
		Element[showTrUp ? 'show' : 'hide'](tr);
	});
}

function hidePublicationPeriod(hidePeriod, firstTime) {
	$$('tr.trPublicationPeriod').each(function(tr) {
		Element[hidePeriod ? 'hide' : 'show'](tr);
	});
	if (!firstTime) {
		if (hidePeriod) {
			disableField($('publicationDate'));
			disableField($('expiryDate'));
		} else {
			enableField($('publicationDate'));
			enableField($('expiryDate'));
		}
	}
}