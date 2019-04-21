package game.model.cards;

import game.controller.GameController;
import game.model.Card;
import game.model.Player;
import game.model.Space;
import game.model.exceptions.GameEndedException;
import game.model.exceptions.PlayerBrokeException;
import game.model.properties.Ship;
import java.util.List;

/**
 * A card that directs the player to a move to a specific space (location) of the game.
 * 
 * @author Ekkart Kindler, ekki@dtu.dk
 * 
 */
public class CardMove extends Card {
	
	private Space target;
	public enum SpecialTargets {NEAREST_SHIP_1, NEAREST_SHIP_2, GO_TO_JAIL, THREE_FORWARDS, THREE_BACKWARDS}
	private SpecialTargets specialTarget;
	List<Space> spaces;

	/** 
	 * Returns the target space to which this card directs the player to go.
	 * 
	 * @return the target of the move
	 */
	public Space getTarget() {
		return target;
	}

	/**
	 * Sets the target space of this card.
	 * 
	 * @param target the new target of the move 
	 */
	public void setTarget(Space target) {
		this.target = target;
	}

    public void setSpecialTarget(SpecialTargets type) {
        this.specialTarget = type;
    }

	public void setSpecialTarget(SpecialTargets type, List<Space> spaces) {
		this.specialTarget = type;
		this.spaces = spaces;
	}
	
	@Override
	public void doAction(GameController controller, Player player) throws PlayerBrokeException, GameEndedException {
		try {
            switch (specialTarget) {
                case NEAREST_SHIP_1:
                    setTargetToNearestShip(player);
                    ((Ship) target).setRent(((Ship) target).getRent() * 2);
                    player.setCurrentPosition(target);
                    target.doAction(controller, player);
                    ((Ship) target).setRent(((Ship) target).getRent() / 2);
                    break;
                case NEAREST_SHIP_2:
                    setTargetToNearestShip(player);
                    controller.moveToSpace(player, target);
                    break;
                case GO_TO_JAIL:
                    controller.gotoJail(player);
                    break;
                case THREE_FORWARDS:
                    controller.moveToSpace(player, spaces.get(player.getCurrentPosition().getIndex() + 3));
                    break;
                case THREE_BACKWARDS:
                    //TODO: Ændre eventuel i View, så spilleren rent faktisk rykker bagud, og ikke hele pladen rundt,
                    // til han ender tre felter bag, hvor han først var.
                    player.setCurrentPosition(spaces.get(player.getCurrentPosition().getIndex() - 3));
                default:
                    controller.moveToSpace(player, target);
                    break;
            }
        } finally {
		    super.doAction(controller, player);
        }

        /*
	    try {
			controller.moveToSpace(player, target);	
		} finally {
			// Make sure that the card is returned to the deck even when
			// an Exception should occur!
			super.doAction(controller, player);
		}*/
	}

	private void setTargetToNearestShip(Player player) {
        Space space = null;
        int i = 1;
        while (!(space instanceof Ship)) {
            space = spaces.get((player.getCurrentPosition().getIndex() + i) % 40);
            i++;
        }
        target = space;
    }
	

	
}
