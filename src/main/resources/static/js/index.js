$(function() { // 在页面加载完以后
	$("#publishBtn").click(publish); // 获取发布按钮,定义一个单击事件
});

function publish() {
	$("#publishModal").modal("hide"); // 将发布框隐藏
	$("#hintModal").modal("show"); // 显示提示框
	setTimeout(function(){ // 过了两秒将其隐藏
		$("#hintModal").modal("hide");
	}, 2000);
}