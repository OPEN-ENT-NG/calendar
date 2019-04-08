import { $, ng} from "entcore";
import { colors } from "../model/constantes/";

export const colorSelector = ng.directive('colorSelector', () =>{

    var templateString = '';
    templateString += '<div class="color-selector">';
    colors.forEach(function (color) {
        templateString += '<div class="color ' + color + '" color="' + color + '"></div>';
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
        link: function ($scope, $element) {

            $element.children('.color').each(function () {
                if ($(this).attr('color') == $scope.ngModel) {
                    $(this).addClass('selected');
                }
            });

            $element.children('.color').on('click', function () {
                var color = $(this).attr('color');
                $scope.ngModel = color;
                $scope.$apply('ngModel');
                $element.children('.color').removeClass('selected');
                $(this).addClass('selected');
            });
        }
    }
});