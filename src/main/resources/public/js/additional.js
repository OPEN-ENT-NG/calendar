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
				console.log('before :' + $scope.ngModel);
				var color = $(this).attr('color');
				$scope.ngModel = color;
				console.log('after : ' + $scope.ngModel);
				$scope.$apply('ngModel');
				$element.children('.color').removeClass('selected');
				$(this).addClass('selected');
			});
		}
	}
});


module.directive('icsImport', function ($compile) {
	return {
		scope: {
			ngModel: '=',
			ngChange: '&',
		},
		transclude: true,
		replace: true,
		restrict: 'E',
		template: '<button><i18n>calendar.import</i18n></button>',
		link: function($scope, $element, $attributes){
			loader.loadFile('/calendar/public/js/ical.js');
			$element.on('click', function() {
				var calendarData = 'BEGIN:VCALENDAR \
				CALSCALE:GREGORIAN \
				PRODID:-//Example Inc.//Example Calendar//EN \
				VERSION:2.0 \
				BEGIN:VEVENT \
				DTSTAMP:20080205T191224Z \
				DTSTART:20081006 \
				SUMMARY:Planning meeting \
				UID:4088E990AD89CB3DBB484909 \
				END:VEVENT \
				END:VCALENDAR';
				var jcalData = ICAL.parse(calendarData);
				console.log(jcalData);
			});
		}
	}
});

module.directive('icsExport', function ($compile) {
	return {
		scope: {
			ngModel: '=',
			ngChange: '&',
		},
		transclude: true,
		replace: true,
		restrict: 'E',
		template: '<button><i18n>calendar.export</i18n></button>',
		link: function($scope, $element, $attributes){
			loader.loadFile('/calendar/public/js/ics-export.js');
			$element.on('click', function() {
				$element.icsExport = ics();
				$scope.ngModel.calendarEvents.forEach(function(calendarEvent) {
					var description = calendarEvent.description ? calendarEvent.description : '';
					var location = calendarEvent.location ? calendarEvent.location : '';
					if (calendarEvent.allday) {
						$element.icsExport.addAllDayEvent(calendarEvent.title, description, location, calendarEvent.startMoment.format("YYYYMMDD"), calendarEvent.endMoment.format("YYYYMMDD"));
					} else {
						$element.icsExport.addEvent(calendarEvent.title, description, location, calendarEvent.startMoment.format("YYYYMMDDTHHmmss"), calendarEvent.endMoment.format("YYYYMMDDTHHmmss"));
					}
				});
				$element.icsExport.download('calendar');
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
			exp: '='
		},
		transclude: true,
		replace: true,
		restrict: 'E',
		template: '<input ng-transclude type="text" data-date-format="dd/mm/yyyy"  />',
		link: function($scope, $element, $attributes){
			$scope.$watch('ngModel', function(newVal){
				if ($scope.ngModel === undefined || $scope.ngModel === null) {
					$scope.ngModel = moment().startOf('day');
				}
				$element.val($scope.ngModel.format('DD/MM/YYYY'));
				
			});
			loader.asyncLoad('/calendar/public/js/bootstrap-datepicker.js', function(){
				$element.datepicker({
						dates: {
							months: moment.months(),
							monthsShort: moment.monthsShort(),
							days: moment.weekdays(),
							daysShort: moment.weekdaysShort(),
							daysMin: moment.weekdaysMin()
						}
					})
					.on('changeDate', function(){
						setTimeout(function(){
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
						}, 10);

						$(this).datepicker('hide');
					});
				$element.datepicker('hide');
			});

			$element.on('focus', function(){
				var that = this;
				$(this).parents('form').on('submit', function(){
					$(that).datepicker('hide');
				});
				$element.datepicker('show');
			});

			$element.on('change', function(){
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
			});

			$element.on('$destroy', function() {
				$element.datepicker('destroy');			
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
		template: "<input type='text'/>",

		link: function($scope, $element, $attributes){
			loader.asyncLoad('/' + infraPrefix + '/public/js/bootstrap-timepicker.js', function(){
				$element.timepicker({
					showMeridian: false,
					defaultTime: 'current',
					disableFocus : true,
					minuteStep: model.timeConfig.interval,
					minHour: model.timeConfig.start_hour,
					maxHour: model.timeConfig.end_hour
				});
			});
			$scope.$watch('ngModel', function(newVal){
				if (newVal) {
					$scope.ngModel = newVal;
					var time = $scope.ngModel.format("HH:mm");
					var hour = $scope.ngModel.hour();
					$element.val($scope.ngModel.format("HH:mm"));
					if( ($scope.ngLimit !== undefined && !newVal.isSame($scope.ngLimit))
							&& ( ($scope.ngBegin === true && newVal.isAfter($scope.ngLimit))
									|| ($scope.ngEnd === true && newVal.isBefore($scope.ngLimit)) )
					){
						$scope.ngLimit = moment(newVal);
					}
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

			$element.on('$destroy', function() {
				$element.timepicker('remove');			
			});
		}
	}
});