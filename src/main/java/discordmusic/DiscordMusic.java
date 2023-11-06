package discordmusic;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordMusic {
    private static final Map<String, Command> commands = new HashMap<>();
    static {
        commands.put("test", event -> event.getMessage()
                .getChannel().block()
                .createMessage("Test!").block());
    }
    public static void main(String[] args) {
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        final AudioPlayer player = playerManager.createPlayer();
        final TrackScheduler scheduler = new TrackScheduler(player);
        AudioProvider provider = new LavaPlayerAudioProvider(player);
        final GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build()
                .login()
                .block();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    final String content = event.getMessage().getContent();
                    System.out.println(content);
                    for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                        if (content.contains(' ' + entry.getKey())) {
                            entry.getValue().execute(event);
                            break;
                        }
                    }
                });
        commands.put("join", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null) {
                        channel.join(spec -> spec.setProvider(provider)).block();
                    }
                }
            }
        });
        commands.put("play", event -> {
            final String content = event.getMessage().getContent();
            final List<String> command = Arrays.asList(content.split("play "));
            event.getMessage().getChannel().block().createMessage("Playing Music By " + event.getMessage().getAuthor().get().getUsername() + " Request").block();
            event.getMessage().getChannel().block().createMessage("Playing: " + command).block();
            playerManager.loadItem(command.get(1), scheduler);
        });

        commands.put("disconnect", event -> {
            // logout
            System.out.println(client.logout());
            client.onDisconnect().block();
            client.onDisconnect();
//            client.logout();
        });


        client.onDisconnect().block();
    }


}