db.getCollection('calendarevent').find().forEach( function(obj) {
    if(typeof obj.calendar == "string"){
        var value = obj.calendar;
        obj.calendar= new Array();
        obj.calendar.push(value);
        db.getCollection('calendarevent').save(obj);
    }
});
