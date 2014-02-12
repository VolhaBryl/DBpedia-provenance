# ...add edit counts from trace to out...

my $path = "fused-data";

@langs = ("en", "ru", "es", "nl", "pt", "pl", "ca", "it", "fr", "de");

foreach $lang (@langs)
{
	$prefix = "http://$lang.dbpedia.org/resource/";

	$fout = "$path/$lang/$lang-big.out";
	$ftrace = "$path/$lang/$lang-big.trace";
	$fnew = "$path/$lang/$lang-big.out.n";

	%editc = ();
	open(trace, $ftrace );
	while ($line = <trace>) 
	{
		if ($line =~ /$prefix/)
		{
			$line =~ s/\n//;
			@tmp = split(/\t/, $line);	
			$sbj = $tmp[0];
			$prop = $tmp[1];
			$edit_cnt = $tmp[2];
			#
			$sbj =~ s/$prefix//;
			$editc{$sbj}{$prop} = $edit_cnt;
		}
	}
	close(trace);
	
	open(out, $fout );
	open(nout, ">$fnew");
	while ($line = <out>) 
	{
		if ($line =~ /$prefix/)
		{
			$line =~ s/\n//;
			@tmp = split(/\t/, $line);	
			$sbj = $tmp[0];
			$sbj =~ s/$prefix//;
			$prop = $tmp[1];
			$edit_cnt = 0;
			if (exists($editc{$sbj}{$prop}))
			{
				$edit_cnt = $editc{$sbj}{$prop};
			}
			print nout "$line\t$edit_cnt\n";
		}
	}
	close(out);
	close(nout);
}