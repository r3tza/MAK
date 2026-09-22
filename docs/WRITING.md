# Writing

If a sentence does not add a fact, a decision, a constraint, an example, or a consequence, cut it. Break any other rule rather than write empty or unnatural prose.

## Always

- Lead with the answer. The first sentence states what is true or what to do. Then give the reason.
- Use one name for one thing. Do not rotate synonyms for variety.
- Prefer short words and concrete verbs. Write the action as a verb, not a noun. Use the active voice when the actor is known.
- Put one idea in a sentence when you state an action or a condition. Treat a long sentence as a warning, not as a style.
- Do not restate the question. Do not open with background, a definition of the topic, or a warm-up.
- Hedge only when the uncertainty is real. Then say what you do not know and what follows from that.
- Prefer names, numbers, paths, conditions, and examples over categories.
- Use format to help scanning: lists for items, bold for labels, headings when the topic changes. Do not decorate.
- Do not use the middle dot `·`, em dash `—`, emojis, or ornamental patterns and stock phrases strongly associated with AI-slop. Use ordinary punctuation and concrete wording.

## Documentation

- Write procedures in the imperative. One instruction per sentence. Put the condition before the command: `If the light is on, stop the engine.`
- Use at most three nouns in a row. If a name is longer, write it in full once, then use a short form.
- Keep the same canonical name for each element in the whole document.
- Write warnings as: risk level → action or condition → consequence.
- Do not drop articles or verbs to look concise.
- Cut background the reader does not need in order to do the task.

## Ordinary prose

Do not apply a fixed word list, a 20-word limit, a ban on `-ing` forms, or a ban on semicolons. Keep a metaphor only when it carries meaning. Cut it when it is ornament.

## Before you send

- For each paragraph: if deleting it would not change the reader's decision or understanding, delete it.
- For documentation: the reader must be able to act without guessing who, what, or when.

## Do not write

Stock filler: *it's important to note*, *in today's landscape*, *robust*, *seamless*, *unlock*, *delve*, *leverage*, *tapestry*, *play a crucial role*, *comprehensive guide*, *in conclusion*.

Default rhythm of the form *not X, but Y*. False balance. Padding so the answer looks complete. Emoji. Motivational closers.

## Examples

Bad: `It is important to note that in many cases this can lead to issues when handling users.`
Good: `If user.id is null, the endpoint returns 500.`

Bad: `Carry out the removal of the cover, then proceed to initiate the reboot process.`
Good: `Remove the cover. Reboot the device.`

Bad: `A pipeline, also known as a flow or process, allows teams to seamlessly leverage automation.`
Good: `The pipeline runs tests on each push.`

Bad: `This is not a configuration problem, but a permissions problem.`
Good: `The 403 comes from file permissions. The config is correct.`

The ban is on the rhythm, not on contrast. When both halves carry a fact, write
two sentences. When only the second half carries a fact, drop the first.
