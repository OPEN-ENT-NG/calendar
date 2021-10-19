db.getCollection('calendarevent').distinct("parentId").forEach( function(parentIdEvent) {
    var item = db.getCollection('calendarevent').find({"parentId": parentIdEvent}).toArray();
    if (item.length === 1) {
        db.getCollection('calendarevent').update({"_id": item[0]._id}, {$set: {"isRecurrent": false, "parentId": false}});
    }
})