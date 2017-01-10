// Like
Parse.Cloud.define("pushLike", function (request, response) {
  var pushQuery = new Parse.Query(Parse.Installation);
  pushQuery.equalTo('userId', request.params.userId);

  var username = request.params.username;
  var result = request.params.result;

  Parse.Push.send({
    where: pushQuery,
    data: {
      alert: "Liked by " + username + ": " + result
    },
  }, {
    useMasterKey: true,
    success: function () {
      response.success("Success!");
    },
    error: function (error) {
      response.error("Error! " + error.message);
    }
  });
});

// Reply
Parse.Cloud.define("pushReply", function (request, response) {
  var pushQuery = new Parse.Query(Parse.Installation);
  pushQuery.equalTo('userId', request.params.userId);

  var username = request.params.username;
  var result = request.params.result;

  Parse.Push.send({
    where: pushQuery,
    data: {
      alert: "Reep from " + username + ": " + result
    },
  }, {
    useMasterKey: true,
    success: function () {
      response.success("Success!");
    },
    error: function (error) {
      response.error("Error! " + error.message);
    }
  });
});

// Rant
Parse.Cloud.define("pushRant", function (request, response) {
  var pushQuery = new Parse.Query(Parse.Installation);
  pushQuery.equalTo('groupId', request.params.groupId);
  pushQuery.notEqualTo("userId", request.params.userId);

  var username = request.params.username;

  Parse.Push.send({
    where: pushQuery,
    data: {
      alert: username + " is starting to rant!"
    },
  }, {
    useMasterKey: true,
    success: function () {
      response.success("Success!");
    },
    error: function (error) {
      response.error("Error! " + error.message);
    }
  });
});

// Rant Stop
Parse.Cloud.define("pushRantStop", function (request, response) {
  var pushQuery = new Parse.Query(Parse.Installation);
  pushQuery.equalTo('groupId', request.params.groupId);
  pushQuery.notEqualTo("userId", request.params.userId);

  var username = request.params.username;

  Parse.Push.send({
    where: pushQuery,
    data: {
      alert: username + " has finished ranting!"
    },
  }, {
    useMasterKey: true,
    success: function () {
      response.success("Success!");
    },
    error: function (error) {
      response.error("Error! " + error.message);
    }
  });
});

// Generic function to push to request.params.userId
Parse.Cloud.define("pushFunction", function (request, response) {
  var pushQuery = new Parse.Query(Parse.Installation);
  pushQuery.equalTo('userId', request.params.userId);

  Parse.Push.send({
    where: pushQuery,
    data: {
      alert: "You got notification."
    },
  }, {
    useMasterKey: true,
    success: function () {
      response.success("Success!");
    },
    error: function (error) {
      response.error("Error! " + error.message);
    }
  });
});