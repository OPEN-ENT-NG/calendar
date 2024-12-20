db.calendarevent.find().forEach(function(event) {
    if (event.description) {
        db.calendarevent.updateOne(
            { _id: event._id },
            { $set: { description: `<div>${event.description}</div>` } }
        );
    }
});