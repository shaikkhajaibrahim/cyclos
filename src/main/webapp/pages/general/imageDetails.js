function azx(o) {
	var a = $H();
	for (p in o) {
		if (isInstance(p, String) && (p.toLowerCase().indexOf("width") >= 0 || p.toLowerCase().indexOf("height") >= 0)) {
			a[p] = o[p];
		}
	}
	return a.toQueryString();
}

Behaviour.register({
	'#closeButton': function(button) {
		button.onclick = function() {
			window.close();
		}
	}, 
	
	'.draggableList': function(list) {
		if (!hasDrag) {
			$A(list.getElementsByTagName("li")).each(function(li) {
				li.style.cursor = "default";
			});
			return;
		}
		try {
			Sortable.create(list);
		} catch (e) {
			//Firefox 2.0 hack - we need to reload the page because of a bug
			self.location.reload(true);
		}
	}
});

document.body.className = "";
document.body.padding = "5px !important";

Event.observe(self, "load", function() {
	var width = 500;
	var height = 570;
	var innerHeight = Element.getDimensions('containerDiv').height;
	window.resizeTo(width, innerHeight + 70);
});