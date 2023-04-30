$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");
	// var token = $("meta[name='_csrf']").attr("content");
	// var header = $("meta[name='_csrf_header']").attr("content");
	// $(document).ajaxSend(function(e, xhr, options){
	//	xhr.setRequestHeader(header, token);
	// });

	var toName = $("#recipient-name").val();
	var content = $("#message-text").val(); // 获取接收者和接收内容
	$.post(
		CONTEXT_PATH + "/letter/send",
		{"toName":toName, "content":content},
		function(data) {
			data = $.parseJSON(data);
			if (data.code == 0) { // 给出提示
				$("#hintBody").text("发送成功！");
			} else {
				$("#hintBody").text(data.msg);
			}
			$("#hintModal").modal("show");
			setTimeout(function() {
				$("#hintModal").modal("hide");
				location.reload(); // 刷新页面
			}, 2000);
		}
	);
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}