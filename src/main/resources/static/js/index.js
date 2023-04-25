$(function() { // 在页面加载完以后
	$("#publishBtn").click(publish); // 获取发布按钮,定义一个单击事件
});

function publish() {
	$("#publishModal").modal("hide"); // 将发布框隐藏
	// 发送异步请求
	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title": title, "content": content},
		function(data) {
			data = $.parseJSON(data);
			// 在提示框中显示返回信息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2秒后,自动隐藏提示框
			setTimeout(function() {
				$("#hintModal").modal("hide");
				// 刷新页面
				if (data.code == 0) {
					window.location.reload();
				}
			}, 2000);
		}
	)
}