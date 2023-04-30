$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	// 关注TA（用户）
	// var token = $("meta[name='_csrf']").attr("content");
	// var header = $("meta[name='_csrf_header']").attr("content");
	// $(document).ajaxSend(function(e, xhr, options){
	//	xhr.setRequestHeader(header, token);
	// });

	if($(btn).hasClass("btn-info")) {
		$.post(
			CONTEXT_PATH + "/follow",
			{"entityType": 3, "entityId": $(btn).prev().val()},
			function(data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					window.location.reload(); // 重新加载界面，不用下面刷新样式
				} else {
					alert(data.msg);
				}
			}
		)
		// $(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
	} else {
		// 取消关注（用户）
		$.post(
			CONTEXT_PATH + "/unfollow",
			{"entityType": 3, "entityId": $(btn).prev().val()},
			function(data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					window.location.reload(); // 重新加载界面，不用下面刷新样式
				} else {
					alert(data.msg);
				}
			}
		)
		// $(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
	}
}