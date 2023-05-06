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

$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax({ // 配置上传给七牛云的参数
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false,
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function(data) { // 成功时处理
            if(data && data.code == 0) { // 成功时,异步请求访问自己的程序,更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) { // 成功更新,重新加载页面
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false; // 不继续原有的submit事件，上面已经处理完成提交事件
}