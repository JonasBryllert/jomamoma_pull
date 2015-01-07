
$( document ).ready(function() {
	$("#more-info-action").click(toggleDisplayInfoDiv);
	$("#less-info-action").click(toggleDisplayInfoDiv);
	
	$("#user-name-info-action-show").click(toggleUserNameInfoDiv);
	$("#user-name-info-action-hide").click(toggleUserNameInfoDiv);
	
	$("#group-name-info-action-show").click(toggleGroupNameInfoDiv);
	$("#group-name-info-action-hide").click(toggleGroupNameInfoDiv);
	
});

function toggleDisplayInfoDiv() {
	$("#info-div").toggleClass("hide");
	$("#more-info-action").toggleClass("hide");
}

function toggleUserNameInfoDiv() {
	$("#user-name-info").toggleClass("hide");
	$("#user-name-info-action-show").toggleClass("hide");
}

function toggleGroupNameInfoDiv() {
	$("#group-name-info").toggleClass("hide");
	$("#group-name-info-action-show").toggleClass("hide");
}

