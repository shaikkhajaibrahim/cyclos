/**
 * MultiDropDown is a multiple choice dropdown combo, generated using javascript
 */
var openedItemsContainer = null;
var MultiDropDown = Class.create();
Object.extend(MultiDropDown.prototype, {
	initialize: function(container, name, values, options) {
		this.name = name || "";

		//Add this to the collection
		document.multiDropDowns.push(this);
		if (!isEmpty(this.name)) {
			document.multiDropDowns[this.name] = this;
		}

		this.container = $(container);
		this.values = values || [];
		this.options = options || {};
		this.options.open = typeof (this.options.open) == "boolean" ? this.options.open : false;
		this.options.disabled = typeof (this.options.disabled) == "boolean" ? this.options.disabled : false;
		this.options.size = this.options.size || 5;
		this.options.minWidth = this.options.minWidth || 50;
		this.options.maxWidth = this.options.maxWidth || 400;
		this.options.height = this.options.height || 17;
		
		//Update the text on document load
		addOnReadyListener(this.render.bind(this));
	},
	
	render: function() {
		var multiDropDown = this;
		this.visibleRows = Math.min(this.options.size, this.values.length);
		
		//Determine the line height
		this.container.setStyle({'opacity':0});
		this.container.innerHTML = "<div><input type='checkbox'>A</div>";
		var height = Element.getDimensions(this.container.firstChild).height;
		if (height > 0) {
			this.lineHeight = height;
			if (is.ie) {
				this.lineHeight += 1;
			}
		}
		this.container.innerHTML = "";
		this.container.setStyle({'opacity':''});

		//If in single field mode, append the div that will contain the selected values
		if (this.options.singleField) {
			var hidden = document.createElement('input');
			hidden.setAttribute("type", "hidden");
			hidden.setAttribute("name", this.name);
			this.valueField = this.container.appendChild(hidden);
		}

		//Create the elements on the div
		this.createHeader();
		this.createItemsContainer();		
		this.createItems();
		
		var _this = this;
		var name = this.name;
		var header = this.header;
		var div = this.itemsContainer;
		var visibleRows = this.visibleRows;
		var options = this.options;

		var toggleFunction = function(event) {
			var isHidden = div.style.display == 'none';
			if (isHidden) {
				div.values = getValue(name);
				Element.show(div);
				if (is.ie || is.opera) {
					Position.prepare();
					var offsetTop = Element.getDimensions(header).height;
					var offsetLeft = is.ie ? 1 : 0;
					Position.clone(header, div, {setHeight: false, offsetTop: offsetTop, offsetLeft: offsetLeft});
				}
			} else {
				Element.hide(div);
				if (options.onchange) {
					var newValues = getValue(name);
					if (newValues && newValues.join) newValues = newValues.join(',');
					var oldValues = div.values;
					if (oldValues && oldValues.join) oldValues = oldValues.join(',');
					if (newValues != div.values) {
						if (typeof options.onchange == 'string') {
							eval(options.onchange);
						} else {
							options.onchange.apply(div);
						}
					}
				}
			}
			
			if (isHidden) {
				if (openedItemsContainer != null) {
					openedItemsContainer.style.display = 'none';
				}
					
				openedItemsContainer = div;
				multiDropDown.updateValues();
			} else {
				openedItemsContainer = null;
			}
			if (event) {
				Event.stop(event);
			}
		};
		
		if (!this.options.open) {
			//Toggle the visibility of the items container on click events
			Event.observe(this.header, 'click', toggleFunction.bindAsEventListener(this.header));
			
			//Hide the div when the user clicks outside
			var toWatch = is.ie && document.body != null ? document.body : self;
			Event.observe(toWatch, "click", function(event) {
				var isHidden = div.style.display == 'none';
				if (!isHidden) {
					toggleFunction(event);
				}
			}.bindAsEventListener(this));
		}
		
		this.updateValues()
	},
	
	createHeader: function() {
		//If the dropdown is open, there is no header
		if (this.options.open) {
			this.header = null;
		} else {
			var style = $H();
			style['padding-left'] = '4px';
			style['clear'] = 'right';
			style['height'] = this.options.height + 'px';
			style['cursor'] = "default";

			var className = "multiDropDownText " + (this.options.disabled ? "multiDropDownDisabled" : "multiDropDown");
			this.header = this.create("div", style, className, this.container);
		}
	},
	
	createItemsContainer: function() {
		var style = $H();
		if (!this.options.open) {
			style['position'] = 'absolute';
		}
		style['display'] = 'none';
		var height = this.lineHeight * Math.max(this.visibleRows, 1);
		if (is.ie && is.version >= 7) {
			height += 5;
		}
		style['height'] = height + 'px';
		style['overflow'] = 'auto';
		//style['overflow-y'] = 'scroll';
		style['margin-top'] = '-1px';

		var className = "multiDropDownText " + (this.options.disabled ? "multiDropDownDisabled" : "multiDropDown");
		this.itemsContainer = this.create("div", style, className, this.header == null ? this.container : this.header);
	}, 
	
	create: function(tagName, style, className, afterNode) {
		var element = document.createElement(tagName);
		element.className = className;
		
		style.map(function(pair) {
			try {
				element.style[pair.key.camelize()] = pair.value;
			} catch (e) {}
		});
		return this.container.appendChild(element);
	}, 
	
	createItems: function() {
		var container = this;
		var itemsContainer = this.itemsContainer;
		var namePart = this.options.singleField ? "" : " name='" + this.name + "'";
		var disabled = this.options.disabled;
		var open = this.options.open;
		
		//Create each checkbox
		this.values.each(function(current, index) {
			var item = itemsContainer.appendChild(document.createElement('div'));
			item.style.cursor = 'default';
			var sb = new StringBuffer();
			sb.append("<div style='white-space:nowrap'>");
			sb.append("<input type='checkbox' class='checkbox' style='vertical-align:middle;' ").append(disabled ? "disabled='disabled'" : "").append(" value='").append(current.value).append("' ").append(namePart).append(current.selected ? " checked='checked'" : "").append(">");
			sb.append(" <span class='multiDropDownText' style='white-space:nowrap;padding-right:3px;vertical-align:middle;'>").append(current.text).append("</span>");
			sb.append("</div>");
			item.innerHTML += sb.toString();
			changeClassOnHover(item, "", "multiDropDownHover");
			var checkbox = item.getElementsByTagName("input")[0];
			
			Event.observe(checkbox, 'click', function(event) {
				if (!checkbox.disabled) {
					if (!open && !container.options.disabled) {
						container.updateValues();
					}
					if (event.stopPropagation) {
						event.stopPropagation();
					} else {
						event.cancelBubble = true;
					}
				}
			});
			
			Event.observe(item, 'click', function(event) {
				if (!checkbox.disabled) {
					if (Event.element(event).tagName.toLowerCase() != 'input') {
						var check = this.getElementsByTagName('input')[0];
						check.checked = !check.checked;
					}
				}
				if (!open && !container.options.disabled) {
					container.updateValues();
				}
				if (event.stopPropagation) {
					event.stopPropagation();
				} else {
					event.cancelBubble = true;
				}
			}.bindAsEventListener(item));
		});
	},
	
	updateValues: function() {
		if (this.options.open) {
			Element.show(this.itemsContainer);
		}
		var className = "multiDropDownText " + (this.options.disabled ? "multiDropDownDisabled" : "multiDropDown");
		if (this.header != null) {
			this.header.className = className;
		}
		this.itemsContainer.className = className;
		
		var selected = [];
		var selectedValues = [];
		var spans = $A(this.itemsContainer.getElementsByTagName("span"))
		var checkWidth = 0;
		if (is.ie) {
			checkWidth = 20;
		}
		var maxTextWidth = 0;

		//Retrieve the selected text and values
		$A(this.itemsContainer.getElementsByTagName("input")).each(function(checkbox, index) {
			var currentSpan = spans[index];
			if (checkWidth == 0) {
				checkWidth = Element.getDimensions(currentSpan.previousSibling.previousSibling).width;
			}
			var width = Element.getDimensions(currentSpan).width;
			maxTextWidth = Math.max(maxTextWidth, width);
			if (checkbox.checked) {
				selected.push(currentSpan.innerHTML);
				if (checkbox.value != "") {
					selectedValues.push(checkbox.value);
				}
			}
		});

		//Update the header with the selected text
		var headerTable = null;
		var headerSpan = null;
		if (this.header != null) {
			if (typeof(mddNoItemsMessage) == "undefined") {
				mddNoItemsMessage = "";
			}
			var emptyLabel = this.options.emptyLabel || mddNoItemsMessage;
						
			if (typeof(mddSingleItemsMessage) == "undefined") {
				mddSingleItemsMessage = "";
			}
			if (typeof(mddMultiItemsMessage) == "undefined") {
				mddMultiItemsMessage = "";
			}
			var text = selected.length == 0 ? emptyLabel : selected.length == 1 ? mddSingleItemsMessage : replaceAll(mddMultiItemsMessage, "#items#", selected.length);
			if (text.length == 0) {
				text = "&nbsp;";
			}
			this.header.style.width = '';
			this.header.innerHTML = "<table style='background-color:transparent;border-spacing:0px;padding:0px;border-collapse:collapse;' cellpadding='0' cellspacing='0' height='100%'><tr><td nowrap class='multiDropDownText' style='padding:0px;padding-right:4px'><span>" + text + "</span></td><td style='padding:0px !important;' width='15' valign='top' align='right'><img style='margin:0px;width:15px;height:17px' src='" + context + "/pages/images/dropdown.gif'></td></tr></table>";
			headerTable = this.header.firstChild;
			headerSpan = headerTable.getElementsByTagName("span")[0];
		}
		
		//If in singleField mode, set the hidden value
		if (this.valueField) {
			this.valueField.value = selectedValues.join(',');
		}
		
		//Ensure the dimensions
		var headerAdjust = 0;
		if (is.ie) {
			headerAdjust = -2;
		}
		if (this.header != null) {
			var headerWidth = Element.getDimensions(headerTable).width;
			if (headerWidth > this.options.maxWidth) {
				headerWidth = this.options.maxWidth;
			} else if (headerWidth < this.options.minWidth) {
				headerWidth = this.options.minWidth;
			}
			this.header.style.width = headerWidth + "px";
			headerTable.style.width = (headerWidth + headerAdjust) + "px";
		}
		var scrollWidth = 30;
		var headerDiff = 7;
		var itemsWidth = checkWidth + maxTextWidth + scrollWidth;
		if (itemsWidth > this.options.maxWidth) {
			itemsWidth = this.options.maxWidth;
		} else if (this.header != null && itemsWidth < headerWidth) {
			itemsWidth = headerWidth + headerDiff;
		} else if (this.header != null && itemsWidth > headerWidth) {
			headerWidth = itemsWidth;
			itemsWidth += headerDiff;
			this.header.style.width = headerWidth + "px";
			headerTable.style.width = (headerWidth + headerAdjust) + "px";
		}
		this.itemsContainer.style.width = (itemsWidth - 3) + "px";
		this.itemsContainer.style.zIndex = 99999;
	},
	
	disable: function() {
		this.options.disabled = true;
		this.updateValues();
	},
	
	enable: function() {
		this.options.disabled = false;
		this.updateValues();
	}
});