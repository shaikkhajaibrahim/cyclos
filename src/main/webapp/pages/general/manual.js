Behaviour.register({
	'#printManualLink': function(a) {
		setPointer(a);
		a.onclick = function() {
			var map = new QueryStringMap();
			var params = $H();
			if (!isEmpty(map.get("admin"))) {
				params.admin = map.get("admin"); 
			}
			if (!isEmpty(map.get("member"))) {
				params.member = map.get("member"); 
			}
			if (!isEmpty(map.get("broker"))) {
				params.broker = map.get("broker"); 
			}
			win = window.open(context + "/do/print/manual?" + params.toQueryString());
		}
	},

	'#printSectionLink': function(a) {
		setPointer(a);
		a.onclick = function() {
			var map = new QueryStringMap();
			var params = $H();
			if (!isEmpty(map.get("admin"))) {
				params.admin = map.get("admin"); 
			}
			if (!isEmpty(map.get("member"))) {
				params.member = map.get("member"); 
			}
			if (!isEmpty(map.get("broker"))) {
				params.broker = map.get("broker"); 
			}
			if (!isEmpty(map.get("page"))) {
				params.page = map.get("page"); 
			}
			win = window.open(context + "/do/print/manual?" + params.toQueryString());
		}
	},
	
	'a.pageLink': function(a) {
		setPointer(a);
		a.onclick = function() {
			var map = new QueryStringMap();
			var params = $H();
			if (!isEmpty(map.get("admin"))) {
				params.admin = map.get("admin"); 
			}
			if (!isEmpty(map.get("member"))) {
				params.member = map.get("member"); 
			}
			if (!isEmpty(map.get("broker"))) {
				params.broker = map.get("broker"); 
			}
			params.page = a.getAttribute('page')
			self.location = pathPrefix + "/manual?" + params.toQueryString();
		}
	},
	
	'a.backLink': function(a) {
		setPointer(a);
		a.onclick = function() {
			history.back()
		}
	}	
})