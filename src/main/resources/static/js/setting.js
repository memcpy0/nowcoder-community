$(function(){
    $("form").submit(check_data); // 提交时检查数据
    $("input").focus(clear_error);
});

function check_data() {
    var pwd1 = $("#new-password").val();
    var pwd2 = $("#confirm-password").val();
    if(pwd1 != pwd2) {
       $("#confirm-password").addClass("is-invalid");
       return false;
    }
    return true;
}

function clear_error() {
    $(this).removeClass("is-invalid");
}