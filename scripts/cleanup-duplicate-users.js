// MongoDB script to cleanup duplicate userId records
// This script merges all FCM tokens from duplicate records into one
// Run with: docker exec -it isu-mongodb-pushnoti mongosh isu_pushnoti_db /scripts/cleanup-duplicate-users.js

print("Starting duplicate user cleanup...");

// Connect to database
db = db.getSiblingDB('isu_pushnoti_db');

// Find all duplicate userIds
const pipeline = [
    {
        $group: {
            _id: "$user_id",
            count: { $sum: 1 },
            ids: { $push: "$_id" },
            allTokens: { $push: "$fcm_tokens" }
        }
    },
    {
        $match: {
            count: { $gt: 1 }
        }
    }
];

const duplicates = db.users.aggregate(pipeline).toArray();

print(`Found ${duplicates.length} duplicate userId(s)`);

duplicates.forEach(dup => {
    print(`\nProcessing userId: ${dup._id}`);
    print(`  Found ${dup.count} duplicate records`);

    // Flatten and deduplicate all FCM tokens
    const allTokens = [];
    dup.allTokens.forEach(tokenArray => {
        if (tokenArray && Array.isArray(tokenArray)) {
            tokenArray.forEach(token => {
                if (token && !allTokens.includes(token)) {
                    allTokens.push(token);
                }
            });
        }
    });

    print(`  Total unique FCM tokens: ${allTokens.length}`);

    // Keep the oldest record (first _id)
    const keepId = dup.ids[0];
    const deleteIds = dup.ids.slice(1);

    print(`  Keeping record: ${keepId}`);
    print(`  Deleting ${deleteIds.length} duplicate(s): ${deleteIds.join(', ')}`);

    // Update the kept record with all merged tokens
    db.users.updateOne(
        { _id: keepId },
        {
            $set: {
                fcm_tokens: allTokens,
                updated_at: new Date()
            }
        }
    );

    // Delete duplicate records
    deleteIds.forEach(id => {
        db.users.deleteOne({ _id: id });
    });

    print(`  ✓ Merged tokens and removed duplicates for userId: ${dup._id}`);
});

// Verify no more duplicates
const remainingDups = db.users.aggregate(pipeline).toArray();
print(`\n=== Cleanup Complete ===`);
print(`Remaining duplicates: ${remainingDups.length}`);

if (remainingDups.length === 0) {
    print("✓ All duplicates cleaned up successfully!");
    print("\nNext step: Create unique index on user_id field");
    print("Run in MongoDB shell:");
    print("  db.users.createIndex({ 'user_id': 1 }, { unique: true })");
} else {
    print("⚠ Warning: Some duplicates remain. Manual intervention may be needed.");
}
