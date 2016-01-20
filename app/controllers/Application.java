/**
 * Copyright 2012-214 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package controllers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;
import play.Logger;
import play.libs.F;
//import play.*;
//import play.mvc.*;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import htwg.se.chess.Init;
import securesocial.core.BasicProfile;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.java.SecureSocial;
import securesocial.core.java.SecuredAction;
import securesocial.core.java.UserAwareAction;
import service.DemoUser;
import views.html.*;


import play.twirl.api.Html;
import play.libs.F.Callback;
import play.libs.F.Callback0;

/**
 * A sample controller
 */
public class Application extends Controller {
	
    public static Logger.ALogger logger = Logger.of("application.controllers.Application");
    private RuntimeEnvironment env;
	Init init = null;
    /**
     * A constructor needed to get a hold of the environment instance.
     * This could be injected using a DI framework instead too.
     *
     * @param env
     */
//    @Inject()
//    public Application (RuntimeEnvironment env) {
//        this.env = env;
//
//    }
    
   

    public Result loggedOutPage() {
        return ok(loggedOutPage.render());
    }

	
	GameInstance game;
	ArrayList<WebSocket.Out<String>> playerList = new ArrayList<WebSocket.Out<String>>();
	List<GameInstance> gameList = new LinkedList<GameInstance>();


	public Result index() {
		return ok(index.render("UChess Titel"));
	}
	
	@SecuredAction
	public Result game() {
		return ok(ng_wui.render());
	}
	
	@SecuredAction
	public WebSocket<String> webSocket() {
		return new WebSocket<String>() {
			public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
				if (playerList.isEmpty()) {
					game = new GameInstance(out);
					playerList.add(out);
					gameList.add(game);
				} else {
					//playerList.clear();
					playerList.remove(0);
					game.setPlayer2(out);
				}

				in.onMessage(event -> {
					game = checkWhichGame(out);
					// System.out.println(event);
					switch (event.substring(0, 4)) {
					case "RESE":
						game.reset(false, out);
						break;
					case "CHAT":
						game.chat(event, out);
						break;
					default:
						game.move(event, out);
					}

				});
				in.onClose(() -> {
					game = checkWhichGame(out);
					game.reset(true, out);
					System.out.println("USER CLOSED CONNECTION:");
				});
			}


			private GameInstance checkWhichGame(Out<String> out) {
				System.out.println("Laufene Spiele: " + gameList.size());
				if (gameList.size() > 1) {
					for (GameInstance gameInstance : gameList) {
						if (gameInstance.player1.equals(out) || gameInstance.player2.equals(out))
							return gameInstance;
					}

				}
				return gameList.get(0);
			}
			
		};
	}
       
    


/* ===============================methods from secureSocial template ================================*/

    /**
     * original method from the secureSocial-Framework
     */
    @SecuredAction
    public Result index_secsoc() {
        if(logger.isDebugEnabled()){
            logger.debug("access granted to index");
        }
        DemoUser user = (DemoUser) ctx().args.get(SecureSocial.USER_KEY);
        return ok(index_secsoc.render(user, SecureSocial.env()));
    }


    /**
     * original method from the secureSocial-Framework
     */
    @UserAwareAction
    public Result userAware() {
        DemoUser demoUser = (DemoUser) ctx().args.get(SecureSocial.USER_KEY);
        String userName ;
        if ( demoUser != null ) {
            BasicProfile user = demoUser.main;
            if ( user.firstName().isDefined() ) {
                userName = user.firstName().get();
            } else if ( user.fullName().isDefined()) {
                userName = user.fullName().get();
            } else {
                userName = "authenticated user";
            }
        } else {
            userName = "guest";
        }
        return ok("Hello " + userName + ", you are seeing a public page");
    }

    /**
     * original method from the secureSocial-Framework
     */
    @SecuredAction(authorization = WithProvider.class, params = {"twitter"})
    public Result onlyTwitter() {
        return ok("You are seeing this because you logged in using Twitter");
    }

    @SecuredAction
    public Result linkResult() {
        DemoUser current = (DemoUser) ctx().args.get(SecureSocial.USER_KEY);
        return ok(linkResult.render(current, current.identities));
    }

    /**
     * Sample use of SecureSocial.currentUser. Access the /current-user to test it
     */
    public F.Promise<Result> currentUser() {
        return SecureSocial.currentUser(env).map( new F.Function<Object, Result>() {
            @Override
            public Result apply(Object maybeUser) throws Throwable {
                String id;

                if ( maybeUser != null ) {
                    DemoUser user = (DemoUser) maybeUser;
                    id = user.main.userId();
                } else {
                    id = "not available. Please log in.";
                }
                return ok("your id is " + id);
            }
        });
    }
}
