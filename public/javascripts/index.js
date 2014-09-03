
$( document ).ready(function() {
	$("#more-info-action").click(toggleDisplayInfoDiv);
	$("#less-info-action").click(toggleDisplayInfoDiv);
	
	$("#user-name-info-action-show").click(toggleUserNameInfoDiv);
	$("#user-name-info-action-hide").click(toggleUserNameInfoDiv);
	
	$("#group-name-info-action-show").click(toggleGroupNameInfoDiv);
	$("#group-name-info-action-hide").click(toggleGroupNameInfoDiv);
	
});

function toggleDisplayInfoDiv() {
	$("#info-div").toggleClass("remove");
	$("#more-info-action").toggleClass("remove");
}

function toggleUserNameInfoDiv() {
	$("#user-name-info").toggleClass("remove");
	$("#user-name-info-action-show").toggleClass("remove");
}

function toggleGroupNameInfoDiv() {
	$("#group-name-info").toggleClass("remove");
	$("#group-name-info-action-show").toggleClass("remove");
}

