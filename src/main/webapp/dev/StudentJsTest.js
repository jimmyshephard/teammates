'use strict';

/* global
clearBootboxButtonClickEvent:false, StudentCommon:false, ensureCorrectModal:false, Const:false, clearBootboxModalStub:false
*/

QUnit.module('student.js');

QUnit.test('bindLinkInUnregisteredPage(selector)', function (assert) {
    clearBootboxButtonClickEvent();
    StudentCommon.bindLinksInUnregisteredPage('#test-bootbox-button');
    ensureCorrectModal(assert, '#test-bootbox-button', Const.ModalDialogHeader.UNREGISTERED_STUDENT, Const.ModalDialogText.UNREGISTERED_STUDENT);
    clearBootboxModalStub();
});