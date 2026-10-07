# Ideas and uses

A ton of possibilities! Capsules can be used as an early backpack moving a chest, as a portable ladder, to deploy
protecting walls to recover during a fight, to move machines or multiblocks… unleash your creativity!

Got a nice trick? Share it on the [Discord](https://discord.gg/wZpBVdr) or in a
[GitHub issue](https://github.com/Lythom/capsule/issues).

## Automation: players are no longer needed

![Automation without players](images/bring-your-base/05-automation.gif)

Players are no longer needed to use capsules :-O

This automation uses:

- A Mechanical User from Extra Utilities 2 to undeploy, then activate the capsule
- A cable from EnderIO to give it to a dispenser
- A dispenser to throw the activated capsule
- A Vacuum Chest from EnderIO to gather the deployed capsule
- Several Sequencers from RFTools to orchestrate the steps

Fun fact: when I put this whole system into a capsule to move it, something unexpected happened. During deployment,
the deployed capsule got sucked in by the system itself and got undeployed by the mechanical user.
To this day, the capsule is still contained in the mechanical user that is contained in the capsule itself, lost in
some unknown time-space universe.

(For those who actually need to retrieve the content after a similar loss, the "unknown time-space universe" is the
folder located at "/structures/capsule".)

_From the [Bring your base! update](Changelog-1.12.2-Bring-your-base-update) (1.12.2)._

> Today (1.16.5 and later) this is simpler: put a linked capsule in a dispenser or a Capture Base and power it with
> redstone. The first signal deploys the content in front of it, the next one undeploys it. See
> [Automation with dispensers](Home#automation-with-dispensers). The templates are now stored in
> `<world save>/capsules`.
