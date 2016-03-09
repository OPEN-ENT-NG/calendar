module.directive('colorSelector', function($compile){
	var colors = model.colors;

	var templateString = '';
	templateString += '<div class="color-selector">';
	colors.forEach(function(color) {
		templateString += '<div class="color '+color+'" color="'+color+'"></div>';
	});
	templateString += '</div>';

	return {
		scope: {
			ngModel: '=',
			ngChange: '&',
			minDate: '=',
			past: '=',
			expObject: '=',
			exp: '='
		},
		replace: true,
		restrict: 'E',
		template: templateString,
		link: function($scope, $element, $attributes){

			$element.children('.color').each(function() {
				if ($(this).attr('color') == $scope.ngModel) {
					$(this).addClass('selected');
				}
			});

			$element.children('.color').on('click', function(){
				var color = $(this).attr('color');
				$scope.ngModel = color;
				$scope.$apply('ngModel');
				$element.children('.color').removeClass('selected');
				$(this).addClass('selected');
			});
		}
	}
});

module.directive('datePickerCalendar', function($compile){
	return {
		scope: {
			ngModel: '=',
			ngChange: '&',
			minDate: '=',
			past: '=',
			expObject: '=',
			exp: '=',
			disable: '=',
			showPanel: '='
		},
		transclude: true,
		replace: true,
		restrict: 'E',
		template: '<input ng-transclude type="text" data-date-format="dd/mm/yyyy" />',
		link: function($scope, $element, $attributes){
			var datePickerElement = null;

			function setNewDate(){
				var date = $element.val().split('/');
				var temp = date[0];
				date[0] = date[1];
				date[1] = temp;
				date = date.join('/');
				var newMoment = moment(new Date(date));

				if ($scope.ngModel) {
					$scope.ngModel.dayOfYear(newMoment.dayOfYear());
					$scope.ngModel.month(newMoment.month());
					$scope.ngModel.year(newMoment.year());
				} else {
					$scope.ngModel = newMoment;
				}
				$scope.$apply('ngModel');
				$scope.$parent.$eval($scope.ngChange);
				$scope.$parent.$apply();
				$element.datepicker('hide');
			}

			$scope.$watch('disable', function(newVal){

				$element.prop('disabled', newVal);
			});

			$scope.$watch('ngModel', function(newVal){

				if ($scope.ngModel === undefined || $scope.ngModel === null) {
					$scope.ngModel = moment().startOf('day');
				}
				$element.val($scope.ngModel.format('DD/MM/YYYY'));
			});

			$element.on('focus', function(){

				var that = this;
				$(this).parents('form').on('submit', function(){
					$(that).datepicker('hide');
				});
				$element.datepicker('show');
			});



			$element.on('change', setNewDate);

			$scope.$watch('showPanel', function(newVal) {

				if (!newVal && newVal!=undefined) {
					if (datePickerElement != null) {
						datePickerElement.datepicker('destroy');
						datePickerElement = null;
					}
				}
				else {
					loader.asyncLoad('/calendar/public/js/bootstrap-datepicker.js', function(){
						datePickerElement = $element.datepicker({
									dates: {
										months: moment.months(),
										monthsShort: moment.monthsShort(),
										days: moment.weekdays(),
										daysShort: moment.weekdaysShort(),
										daysMin: moment.weekdaysMin()
									},
									format: 'dd/mm/yyyy',
									weekStart: 1
								})
								.on('changeDate', function(){
									setTimeout(setNewDate, 10);

									$(this).datepicker('hide');
								});
						$element.datepicker('hide');
					});
				}
			});

			$element.on('$destroy', function() {

				if (datePickerElement != null) {
					datePickerElement.datepicker('destroy');
				}
			});
		}
	}
});

module.directive('timePickerCalendar', function($compile){
	return {
		scope: {
			ngModel: '=',
			ngBegin: '=',
			ngEnd: '=',
			ngLimit: '='
		},
		transclude: true,
		replace: true,
		restrict: 'E',
		template: "<input type='text' />",
		link: function($scope, $element, $attributes){
			var hideFunction = function(e){
				var timepicker = $element.data('timepicker');
				if(!timepicker || $element[0] === e.target || $('.bootstrap-timepicker-widget').find(e.target).length !== 0){
					return;
				}
				timepicker.hideWidget();
			};
			$('body, lightbox').on('click', hideFunction);
			$('body, lightbox').on('focusin', hideFunction);
			loader.asyncLoad('/' + infraPrefix + '/public/js/bootstrap-timepicker.js', function(){
				$element.timepicker({
					showMeridian: false,
					defaultTime: 'current',
					minuteStep: model.timeConfig.intervalTime,
					minHour: model.timeConfig.start_hour,
					maxHour: model.timeConfig.end_hour
				});
			});

			$scope.$watch('ngModel', function(newVal){
				$scope.ngModel = newVal;
				$element.val($scope.ngModel.format("HH:mm"));
				if( ($scope.ngLimit !== undefined && !newVal.isSame($scope.ngLimit))
						&& ( ($scope.ngBegin === true && newVal.isAfter($scope.ngLimit))
						|| ($scope.ngEnd === true && newVal.isBefore($scope.ngLimit)) )
				){
					$scope.ngLimit = moment(newVal);
				}
			});

			$element.on('change', function(){
				var time = $element.val().split(':');
				$scope.ngModel = moment($scope.ngLimit);
				$scope.ngModel.set('hour', time[0]);
				$scope.ngModel.set('minute', time[1]);
				$scope.$apply('ngModel');
				$scope.$parent.$eval($scope.ngChange);
				$scope.$parent.$apply();
			});

			$element.on('focus', function() {
				$element.timepicker('updateFromElementVal');
			});

			$element.on('show.timepicker', function() {
				var timepicker = $element.data('timepicker');
				if (! timepicker){
					return;
				}
				var lightbox = $element.parents().find('lightbox');
				if(lightbox) {
					_.each(lightbox.find('.lightbox-view, .lightbox-background'), function(zone) {
						$(zone).on('mousedown.timepicker, touchend.timepicker', function(e) {
							if (!($element.parent().find(e.target).length ||
									timepicker.$widget.is(e.target) ||
									timepicker.$widget.find(e.target).length)) {
								timepicker.hideWidget();
							}
						});
					});
				}
			});

			$element.on('hide.timepicker', function() {
				var lightbox = $element.parents().find('lightbox');
				if(lightbox) {
					_.each(lightbox.find('.lightbox-view, .lightbox-background'), function(zone) {
						$(zone).off('mousedown.timepicker, touchend.timepicker');
					});
				}
			});

			$element.on('$destroy', function(){
				$element.timepicker('remove');
			});
		}
	}
});