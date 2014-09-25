$(document).ready(function() {

    //Locate all chart containers...use id attr as the key
    $("#commandField").keypress(function(e) {
        if (e.keyCode == 13) {
            e.preventDefault();

            var commandForm = $("#commandForm .form-group");
            commandForm.removeClass();
            commandForm.addClass("form-group has-feedback");
            $("span.form-control-feedback").remove();

            var commandField = $(this);
            var command = commandField.val();

            if (command.length > 0) {

                $.post( commandField.data("command-url"), {"command": command}, function() {
                    commandForm.addClass("has-success");
                    commandForm.append('<span class="glyphicon glyphicon-ok form-control-feedback"></span>');
                })
                    .fail(function() {
                        commandForm.addClass("has-error");
                        commandForm.append('<span class="glyphicon glyphicon-remove form-control-feedback"></span>');
                    });
            }

            return false;
        }
    });


});

