package com.thistestuser.unsecureswf.transformers;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class Transformers
{
	public static void transformSwap(List<String> list)
	{
		for(int i = 0; i < list.size() - 2; i++)
		{
			String s1 = list.get(i);
			if(s1.trim().equals(""))
				continue;
			Entry<Integer, String> en2 = getNext(list, i, 1, false);
			Entry<Integer, String> en3 = getNext(list, i, 2, false);
			if(en3 != null && s1.trim().equals("pushfalse") && en2.getValue().trim().equals("pushtrue") 
				&& en3.getValue().trim().equals("swap"))
			{
				Collections.swap(list, i, en2.getKey());
				list.remove(en3.getKey().intValue());
			}
		}
	}
	
	public static void transformContJump(List<String> list)
	{
		for(int i = 0; i < list.size() - 1; i++)
		{
			String s1 = list.get(i);
			Entry<Integer, String> en2 = getNext(list, i, 1, false);
			if(en2 != null)
			{
				String[] split = s1.trim().split("\\s+");
				String nextTrim = en2.getValue().trim();
				if(nextTrim.matches("^L[0-9]+:$")
					&& split.length == 2 && split[0].equals("jump"))
				{
					if(split[1].equals(nextTrim.substring(0, nextTrim.length() - 1)))
						list.remove(i);
					else
					{
						int offset = 1;
						while(true)
						{
							offset++;
							Entry<Integer, String> enNext = getNext(list, i, offset, false);
							String nextTrim2 = enNext.getValue().trim();
							if(!nextTrim2.matches("^L[0-9]+:$"))
								break;
							if(split[1].equals(nextTrim2.substring(0, nextTrim.length() - 1)))
							{
								list.remove(i);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	public static void transformPop(List<String> list)
	{
		for(int i = 0; i < list.size() - 1; i++)
		{
			String s1 = list.get(i);
			Entry<Integer, String> en2 = getNext(list, i, 1, false);
			if(en2 != null)
			{
				String[] split = s1.trim().split("\\s+");
				String nextTrim = en2.getValue().trim();
				if(split.length >= 2 && split[0].equals("newfunction") && nextTrim.equals("pop"))
				{
					list.remove(en2.getKey().intValue());
					list.remove(i);
				}
			}
		}
	}
	
	public static void transformFakeJumps(List<String> list)
	{
		int index = 0;
		String first = list.get(0);
		if(first.equals(""))
		{
			Entry<Integer, String> en = getNext(list, 0, 1, true);
			first = en.getValue();
			index = en.getKey();
		}
		Entry<Integer, String> enNext = getNext(list, index, 1, true);
		int mode = -1;
		if(first.trim().equals("pushfalse") && enNext.getValue().trim().equals("pushtrue"))
			mode = 0;
		else if(first.trim().equals("pushtrue") && enNext.getValue().trim().equals("pushfalse"))
			mode = 1;
		if(mode == -1)
			return;
		
		Entry<Integer, String> setLoc1 = getNext(list, enNext.getKey(), 1, true);
		Entry<Integer, String> setLoc2 = getNext(list, enNext.getKey(), 2, true);
		
		int loc1 = -1;
		int loc2 = -1;
		
		if(setLoc1.getValue().trim().matches("^setlocal[0-9]$"))
			loc1 = Integer.parseInt(Character.toString(setLoc1.getValue().trim().charAt(setLoc1.getValue().trim().length() - 1)));
		else
		{
			String[] split = setLoc1.getValue().trim().split("\\s+");
			if(split.length == 2 && split[0].equals("setlocal") && isInteger(split[1]))
				loc1 = Integer.parseInt(split[1]);
		}
		if(setLoc2.getValue().trim().matches("^setlocal[0-9]$"))
			loc2 = Integer.parseInt(Character.toString(setLoc2.getValue().trim().charAt(setLoc2.getValue().trim().length() - 1)));
		else
		{
			String[] split = setLoc2.getValue().trim().split("\\s+");
			if(split.length == 2 && split[0].equals("setlocal") && isInteger(split[1]))
				loc2 = Integer.parseInt(split[1]);
		}

		if(loc1 == -1 || loc2 == -1)
			return;
		
		boolean modified;
		do
		{
			modified = false;
			for(int i = 0; i < list.size() - 1; i++)
			{
				int local = -1;
				String s = list.get(i);
				if(s.trim().matches("^getlocal[0-9]$"))
					local = Integer.parseInt(Character.toString(s.trim().charAt(s.trim().length() - 1)));
				else
				{
					String[] split = s.trim().split("\\s+");
					if(split.length == 2 && split[0].equals("getlocal") && isInteger(split[1]))
						local = Integer.parseInt(split[1]);
				}
				if(local != loc1 && local != loc2)
					continue;
				Entry<Integer, String> next = getNext(list, i, 1, true);
				if(next == null)
					throw new RuntimeException("Unexpected end of code");
				String[] nextSplit = next.getValue().trim().split("\\s+");
				if(nextSplit.length == 2 && (nextSplit[0].equals("iftrue") || nextSplit[0].equals("iffalse"))
					&& nextSplit[1].matches("^L[0-9]+$"))
				{
					boolean ifstatement = false;
					if(nextSplit[0].equals("iftrue"))
						ifstatement = true;
					if(local == loc1)
						if((ifstatement && mode == 0) || (!ifstatement && mode == 1))
						{
							String replace = ifstatement ? next.getValue().replace("iftrue", "jump  ")
								: next.getValue().replace("iffalse", "jump   ");
							list.set(next.getKey(), replace);
							list.remove(i);
						}else
						{
							list.remove(next.getKey().intValue());
							list.remove(i);
						}
					else
						if((!ifstatement && mode == 0) || (ifstatement && mode == 1))
						{
							String replace = ifstatement ? next.getValue().replace("iftrue", "jump  ")
								: next.getValue().replace("iffalse", "jump   ");
							list.set(next.getKey(), replace);
							list.remove(i);
						}else
						{
							list.remove(next.getKey().intValue());
							list.remove(i);
						}
					modified = true;
				}else if(nextSplit.length == 1 && nextSplit[0].equals("not"))
				{
					Entry<Integer, String> next2 = getNext(list, next.getKey(), 1, true);
					String[] nextSplit2 = next2.getValue().trim().split("\\s+");
					if(nextSplit2.length == 2 && (nextSplit2[0].equals("iftrue") || nextSplit2[0].equals("iffalse"))
						&& nextSplit2[1].matches("^L[0-9]+$"))
					{
						boolean ifstatement = false;
						if(nextSplit2[0].equals("iftrue"))
							ifstatement = true;
						//Negated! (Writer also flipped)
						ifstatement = !ifstatement;
						if(local == loc1)
							if((ifstatement && mode == 0) || (!ifstatement && mode == 1))
							{
								String replace = !ifstatement ? next2.getValue().replace("iftrue", "jump  ")
									: next2.getValue().replace("iffalse", "jump   ");
								list.set(next2.getKey(), replace);
								list.remove(next.getKey().intValue());
								list.remove(i);
							}else
							{
								list.remove(next2.getKey().intValue());
								list.remove(next.getKey().intValue());
								list.remove(i);
							}
						else
							if((!ifstatement && mode == 0) || (ifstatement && mode == 1))
							{
								String replace = !ifstatement ? next2.getValue().replace("iftrue", "jump  ")
									: next2.getValue().replace("iffalse", "jump   ");
								list.set(next2.getKey(), replace);
								list.remove(next.getKey().intValue());
								list.remove(i);
							}else
							{
								list.remove(next2.getKey().intValue());
								list.remove(next.getKey().intValue());
								list.remove(i);
							}
						modified = true;
					}
				}else if(nextSplit.length == 1 && nextSplit[0].equals("dup"))
				{
					Entry<Integer, String> next2 = getNext(list, i, 2, true);
					Entry<Integer, String> next3 = getNext(list, i, 3, true);
					Entry<Integer, String> next4 = getNext(list, i, 4, true);
					Entry<Integer, String> next5 = getNext(list, i, 5, true);
					Entry<Integer, String> next6 = getNext(list, i, 6, true);
					if(next6 == null)
						throw new RuntimeException("Unexpected null");
					String[] nextSplit2 = next2.getValue().trim().split("\\s+");
					String[] nextSplit6 = next6.getValue().trim().split("\\s+");
					if(nextSplit2.length == 2 && (nextSplit2[0].equals("iftrue") || nextSplit2[0].equals("iffalse"))
						&& nextSplit2[1].matches("^L[0-9]+$") && next3.getValue().trim().equals("pop")
						&& next4.getValue().trim().matches("^getlocal[0-9]$")
						&& next5.getValue().trim().equals("convert_b")
						&& nextSplit6.length == 2 && (nextSplit6[0].equals("iftrue") || nextSplit6[0].equals("iffalse"))
						&& nextSplit6[1].matches("^L[0-9]+$"))
					{
						Entry<Integer, String> labelAfter = getNext(list, next5.getKey(), 1, false);
						if(!labelAfter.getValue().startsWith("L"))
							throw new RuntimeException("Unexpected pattern (missing label)");
						if(!nextSplit2[1].equals(labelAfter.getValue().trim().substring(0, labelAfter.getValue().trim().length() - 1)))
							throw new RuntimeException("Unexpected pattern (Invaild jump)");
						if(local == loc1)
						{
							if(mode == 0 && (nextSplit2[0].equals("iffalse") || nextSplit6[0].equals("iftrue")))
								throw new RuntimeException("Unexpected pattern (1)");
							if(mode == 1 && (nextSplit2[0].equals("iftrue") || nextSplit6[0].equals("iffalse")))
								throw new RuntimeException("Unexpected pattern (2)");
							Entry<Integer, String> labelMark = getNext(list, labelAfter.getKey(), 1, false);
							list.remove(next6.getKey().intValue());
							if(labelMark.getValue().trim().equals("label"))
								list.remove(labelMark.getKey().intValue());
							list.remove(labelAfter.getKey().intValue());
							list.remove(next5.getKey().intValue());
							list.remove(next4.getKey().intValue());
							list.remove(next3.getKey().intValue());
							list.remove(next2.getKey().intValue());
							list.remove(next.getKey().intValue());
							list.remove(i);
						}else
						{
							if(mode == 0 && (nextSplit2[0].equals("iftrue") || nextSplit6[0].equals("iffalse")))
								throw new RuntimeException("Unexpected pattern (3)");
							if(mode == 1 && (nextSplit2[0].equals("iffalse") || nextSplit6[0].equals("iftrue")))
								throw new RuntimeException("Unexpected pattern (4)");
							Entry<Integer, String> labelMark = getNext(list, labelAfter.getKey(), 1, false);
							list.remove(next6.getKey().intValue());
							if(labelMark.getValue().trim().equals("label"))
								list.remove(labelMark.getKey().intValue());
							list.remove(labelAfter.getKey().intValue());
							list.remove(next5.getKey().intValue());
							list.remove(next4.getKey().intValue());
							list.remove(next3.getKey().intValue());
							list.remove(next2.getKey().intValue());
							list.remove(next.getKey().intValue());
							list.remove(i);
						}
						modified = true;
					}
				}
			}
			Transformers.transformContJump(list);
		}while(modified);
		
		//Remove dead code lines
		for(int i = 0; i < list.size() - 1; i++)
		{
			String s = list.get(i);
			String[] split = s.trim().split("\\s+");
			if(split.length == 2 && split[0].equals("jump"))
			{
				loop:
				while(true)
				{
					Entry<Integer, String> next = getNext(list, i, 1, false);
					if(next == null)
						break;
					if(next.getValue().trim().matches("^L[0-9]+:$"))
					{
						for(int i1 = 0; i1 < list.size(); i1++)
						{
							String str = list.get(i1);
							String[] splitStr = str.trim().split("\\s+");
							if(splitStr.length == 2 && splitStr[1].equals(next.getValue().trim().substring(0, 
								next.getValue().trim().length() - 1)))
								break loop;
							if(splitStr.length > 1 && splitStr[0].equals("lookupswitch"))
								for(int i2 = 1; i2 < splitStr.length; i2++)
								{
									String compare = i2 == 2 ? splitStr[i2].substring(1, splitStr[i2].length() - 1)
										: splitStr[i2].substring(0, splitStr[i2].length() - 1);
									if(compare.equals(next.getValue().trim().substring(0, 
											next.getValue().trim().length() - 1)))
										break loop;
								}
						}
						i++;
						continue loop;
					}
					list.remove(next.getKey().intValue());
				}
			}
		}
		
		//Scan again for any getlocals
		for(int i = 0; i < list.size() - 1; i++)
		{
			int local = -1;
			String s = list.get(i);
			if(s.trim().matches("^getlocal[0-9]$"))
				local = Integer.parseInt(Character.toString(s.trim().charAt(s.trim().length() - 1)));
			else
			{
				String[] split = s.trim().split("\\s+");
				if(split.length == 2 && split[0].equals("getlocal") && isInteger(split[1]))
					local = Integer.parseInt(split[1]);
			}
			
			if(local == loc1 || local == loc2)
				throw new RuntimeException("Unexpected pattern (call not cleared)" + local);
		}
		
		list.remove(setLoc2.getKey().intValue());
		list.remove(setLoc1.getKey().intValue());
		list.remove(enNext.getKey().intValue());
		list.remove(index);
	}
	
	public static boolean transformFakeLocals(List<String> list, int params)
	{
		Set<Integer> initializedLocals = new HashSet<>();
		for(int i = 0; i <= params; i++)
			initializedLocals.add(i);
		for(String s : list)
			if(s.trim().matches("^setlocal[0-9]$"))
				initializedLocals.add(Integer.parseInt(Character.toString(
					s.trim().charAt(s.trim().length() - 1))));
			else
			{
				String[] split = s.trim().split("\\s+");
				if(split.length == 2 && split[0].equals("setlocal") && isInteger(split[1]))
					initializedLocals.add(Integer.parseInt(split[1]));
			}
		boolean returnModified = false;
		boolean modified;
		do
		{
			modified = false;
			loop:
			for(int i = 0; i < list.size() - 1; i++)
			{
				int local = -1;
				String s = list.get(i);
				if(s.trim().matches("^getlocal[0-9]$"))
					local = Integer.parseInt(Character.toString(s.trim().charAt(s.trim().length() - 1)));
				else
				{
					String[] split = s.trim().split("\\s+");
					if(split.length == 2 && split[0].equals("getlocal") && isInteger(split[1]))
						local = Integer.parseInt(split[1]);
				}
				boolean push = false;
				if(local == -1 && !s.trim().equals("pushfalse") && !s.trim().equals("pushtrue"))
					continue;
				if(s.trim().equals("pushtrue"))
					push = true;
				if(initializedLocals.contains(local))
					continue;
				Entry<Integer, String> next = getNext(list, i, 1, true);
				int incr = 1;
				while(true)
				{
					Entry<Integer, String> labelNext  = getNext(list, i, incr, false);
					if(labelNext.getKey().equals(next.getKey()) && labelNext.getValue().equals(next.getValue()))
						break;
					if(labelNext.getValue().trim().matches("^L[0-9]+:$"))
						for(int i1 = 0; i1 < list.size(); i1++)
						{
							String str = list.get(i1);
							String[] splitStr = str.trim().split("\\s+");
							if(splitStr.length == 2 && splitStr[1].equals(labelNext.getValue().trim().substring(0, 
								labelNext.getValue().trim().length() - 1)))
								continue loop;
							if(splitStr.length > 1 && splitStr[0].equals("lookupswitch"))
								for(int i2 = 1; i2 < splitStr.length; i2++)
								{
									String compare = i2 == 2 ? splitStr[i2].substring(1, splitStr[i2].length() - 1)
										: splitStr[i2].substring(0, splitStr[i2].length() - 1);
									if(compare.equals(labelNext.getValue().trim().substring(0, 
										labelNext.getValue().trim().length() - 1)))
										continue loop;
								}
						}
					incr++;
				}
				if(next == null)
					throw new RuntimeException("Unexpected end of code");
				String[] nextSplit = next.getValue().trim().split("\\s+");
				if(nextSplit.length == 2 && (nextSplit[0].equals("iftrue") || nextSplit[0].equals("iffalse"))
					&& nextSplit[1].matches("^L[0-9]+$"))
				{
					boolean ifstatement = false;
					if(nextSplit[0].equals("iftrue"))
						ifstatement = true;
					if(push ? ifstatement : !ifstatement)
					{
						String replace = ifstatement ? next.getValue().replace("iftrue", "jump  ")
							: next.getValue().replace("iffalse", "jump   ");
						list.set(next.getKey(), replace);
						list.remove(i);
					}else
					{
						list.remove(next.getKey().intValue());
						list.remove(i);
					}
					modified = true;
				}else if(nextSplit.length == 1 && nextSplit[0].equals("not"))
				{
					Entry<Integer, String> next2 = getNext(list, next.getKey(), 1, true);
					int incr2 = 1;
					while(true)
					{
						Entry<Integer, String> labelNext  = getNext(list, next.getKey(), incr2, false);
						if(labelNext.getKey().equals(next2.getKey()) && labelNext.getValue().equals(next2.getValue()))
							break;
						if(labelNext.getValue().trim().matches("^L[0-9]+:$"))
							for(int i1 = 0; i1 < list.size(); i1++)
							{
								String str = list.get(i1);
								String[] splitStr = str.trim().split("\\s+");
								if(splitStr.length == 2 && splitStr[1].equals(labelNext.getValue().trim().substring(0, 
									labelNext.getValue().trim().length() - 1)))
									continue loop;
								if(splitStr.length > 1 && splitStr[0].equals("lookupswitch"))
									for(int i2 = 1; i2 < splitStr.length; i2++)
									{
										String compare = i2 == 2 ? splitStr[i2].substring(1, splitStr[i2].length() - 1)
											: splitStr[i2].substring(0, splitStr[i2].length() - 1);
										if(compare.equals(labelNext.getValue().trim().substring(0, 
											labelNext.getValue().trim().length() - 1)))
											continue loop;
									}
							}
						incr2++;
					}
					String[] nextSplit2 = next2.getValue().trim().split("\\s+");
					if(nextSplit2.length == 2 && (nextSplit2[0].equals("iftrue") || nextSplit2[0].equals("iffalse"))
						&& nextSplit2[1].matches("^L[0-9]+$"))
					{
						boolean ifstatement = false;
						if(nextSplit2[0].equals("iftrue"))
							ifstatement = true;
						//Negated! (Writer also flipped)
						ifstatement = !ifstatement;
						if(push ? ifstatement : !ifstatement)
						{
							String replace = !ifstatement ? next2.getValue().replace("iftrue", "jump  ")
								: next2.getValue().replace("iffalse", "jump   ");
							list.set(next2.getKey(), replace);
							list.remove(next.getKey().intValue());
							list.remove(i);
						}else
						{
							list.remove(next2.getKey().intValue());
							list.remove(next.getKey().intValue());
							list.remove(i);
						}
						modified = true;
					}
				}
			}
			if(modified)
				returnModified = true;
		}while(modified);
		return returnModified;
	}
	
	public static boolean transformRedundantJump(List<String> list, int params)
	{
		boolean modified;
		boolean returnModified = false;
		/*
		 * Plan:
		 * do loop
		 * 1. Convert dup - iftrue (include not) to jump
		 * 2. Clear after jump (if modified)
		 * 3. Clear push - pop
		 *  end do loop
		 */
		do
		{
			modified = false;
			for(int i = 0; i < list.size() - 1; i++)
			{
				String s = list.get(i);
				if(s.trim().equals("pushtrue") || s.trim().equals("pushfalse"))
				{
					Entry<Integer, String> next = getNext(list, i, 1, true);
					if(next.getValue().trim().equals("not"))
					{
						String replace = s.trim().equals("pushtrue") ? s.replace("pushtrue", "pushfalse")
							: s.replace("pushfalse", "pushtrue");
						list.remove(next.getKey().intValue());
						list.set(i, replace);
					}
				}
			}
			loop:
			for(int i = 0; i < list.size() - 1; i++)
			{
				String s = list.get(i);
				if(s.trim().equals("pushtrue") || s.trim().equals("pushfalse"))
				{
					Entry<Integer, String> next = getNext(list, i, 1, true);
					int incr = 1;
					while(true)
					{
						Entry<Integer, String> labelNext  = getNext(list, i, incr, false);
						if(labelNext.getKey().equals(next.getKey()) && labelNext.getValue().equals(next.getValue()))
							break;
						if(labelNext.getValue().trim().matches("^L[0-9]+:$"))
							for(int i1 = 0; i1 < list.size(); i1++)
							{
								String str = list.get(i1);
								String[] splitStr = str.trim().split("\\s+");
								if(splitStr.length == 2 && splitStr[1].equals(labelNext.getValue().trim().substring(0, 
									labelNext.getValue().trim().length() - 1)))
									continue loop;
								if(splitStr.length > 1 && splitStr[0].equals("lookupswitch"))
									for(int i2 = 1; i2 < splitStr.length; i2++)
									{
										String compare = i2 == 2 ? splitStr[i2].substring(1, splitStr[i2].length() - 1)
											: splitStr[i2].substring(0, splitStr[i2].length() - 1);
										if(compare.equals(labelNext.getValue().trim().substring(0, 
											labelNext.getValue().trim().length() - 1)))
											continue loop;
									}
							}
						incr++;
					}
					if(next.getValue().trim().equals("dup"))
					{
						Entry<Integer, String> next2 = getNext(list, next.getKey(), 1, true);
						int incr2 = 1;
						while(true)
						{
							Entry<Integer, String> labelNext  = getNext(list, next.getKey(), incr2, false);
							if(labelNext.getKey().equals(next2.getKey()) && labelNext.getValue().equals(next2.getValue()))
								break;
							if(labelNext.getValue().trim().matches("^L[0-9]+:$"))
								for(int i1 = 0; i1 < list.size(); i1++)
								{
									String str = list.get(i1);
									String[] splitStr = str.trim().split("\\s+");
									if(splitStr.length == 2 && splitStr[1].equals(labelNext.getValue().trim().substring(0, 
										labelNext.getValue().trim().length() - 1)))
										continue loop;
									if(splitStr.length > 1 && splitStr[0].equals("lookupswitch"))
										for(int i2 = 1; i2 < splitStr.length; i2++)
										{
											String compare = i2 == 2 ? splitStr[i2].substring(1, splitStr[i2].length() - 1)
												: splitStr[i2].substring(0, splitStr[i2].length() - 1);
											if(compare.equals(labelNext.getValue().trim().substring(0, 
												labelNext.getValue().trim().length() - 1)))
												continue loop;
										}
								}
							incr2++;
						}
						String[] nextSplit2 = next2.getValue().trim().split("\\s+");
						if(nextSplit2.length == 2 && (nextSplit2[0].equals("iftrue") || nextSplit2[0].equals("iffalse"))
							&& nextSplit2[1].matches("^L[0-9]+$"))
						{
							if((s.trim().equals("pushtrue") && nextSplit2[0].equals("iftrue"))
								|| (s.trim().equals("pushfalse") && nextSplit2[0].equals("iffalse")))
							{
								String replace = nextSplit2[0].equals("iftrue") ? next2.getValue().replace("iftrue", "jump  ")
									: next2.getValue().replace("iffalse", "jump   ");
								list.set(next2.getKey().intValue(), replace);
								list.remove(next.getKey().intValue());
							}else
							{
								list.remove(next2.getKey().intValue());
								list.remove(next.getKey().intValue());
							}
							modified = true;
						}
					}
				}
			}
			if(modified)
				//Remove dead code lines
				for(int i = 0; i < list.size() - 1; i++)
				{
					String s = list.get(i);
					String[] split = s.trim().split("\\s+");
					if(split.length == 2 && split[0].equals("jump"))
					{
						loop:
						while(true)
						{
							Entry<Integer, String> next = getNext(list, i, 1, false);
							if(next == null)
								break;
							if(next.getValue().trim().matches("^L[0-9]+:$"))
							{
								for(int i1 = 0; i1 < list.size(); i1++)
								{
									String str = list.get(i1);
									String[] splitStr = str.trim().split("\\s+");
									if(splitStr.length == 2 && splitStr[1].equals(next.getValue().trim().substring(0, 
										next.getValue().trim().length() - 1)))
										break loop;
									if(splitStr.length > 1 && splitStr[0].equals("lookupswitch"))
										for(int i2 = 1; i2 < splitStr.length; i2++)
										{
											String compare = i2 == 2 ? splitStr[i2].substring(1, splitStr[i2].length() - 1)
												: splitStr[i2].substring(0, splitStr[i2].length() - 1);
											if(compare.equals(next.getValue().trim().substring(0, 
													next.getValue().trim().length() - 1)))
												break loop;
										}
								}
								i++;
								continue loop;
							}
							list.remove(next.getKey().intValue());
						}
					}
				}
			loop:
			for(int i = 0; i < list.size() - 1; i++)
			{
				String s = list.get(i);
				if(s.trim().equals("pushtrue") || s.trim().equals("pushfalse"))
				{
					Entry<Integer, String> next = getNext(list, i, 1, true);
					int incr = 1;
					while(true)
					{
						Entry<Integer, String> labelNext  = getNext(list, i, incr, false);
						if(labelNext.getKey().equals(next.getKey()) && labelNext.getValue().equals(next.getValue()))
							break;
						if(labelNext.getValue().trim().matches("^L[0-9]+:$"))
							for(int i1 = 0; i1 < list.size(); i1++)
							{
								String str = list.get(i1);
								String[] splitStr = str.trim().split("\\s+");
								if(splitStr.length == 2 && splitStr[1].equals(labelNext.getValue().trim().substring(0, 
									labelNext.getValue().trim().length() - 1)))
									continue loop;
								if(splitStr.length > 1 && splitStr[0].equals("lookupswitch"))
									for(int i2 = 1; i2 < splitStr.length; i2++)
									{
										String compare = i2 == 2 ? splitStr[i2].substring(1, splitStr[i2].length() - 1)
											: splitStr[i2].substring(0, splitStr[i2].length() - 1);
										if(compare.equals(labelNext.getValue().trim().substring(0, 
											labelNext.getValue().trim().length() - 1)))
											continue loop;
									}
							}
						incr++;
					}
					if(next.getValue().trim().equals("pop"))
					{
						list.remove(next.getKey().intValue());
						list.remove(i);
						modified = true;
					}
				}
			}
			if(modified)
			{
				Transformers.transformContJump(list);
				Transformers.transformFakeLocals(list, params);
				returnModified = true;
			}
		}while(modified);
		return returnModified;
	}
	
	private static Entry<Integer, String> getNext(List<String> list, int index, int amount, boolean skipLabel)
	{
		boolean reverse = false;
		if(amount < 0)
		{
			amount = -amount;
			reverse = true;
		}
		String next = null;
		for(int i = 0; i < amount; i++)
		{
			while(true)
			{
				if(reverse) 
					index--;
				else
					index++;
				if(index >= list.size())
					return null;
				String s = list.get(index);
				if(s.trim().equals(""))
					continue;
				if(skipLabel && s.trim().matches("^L[0-9]+:$"))
					continue;
				if(skipLabel && s.trim().equals("label"))
					continue;
				next = s;
				break;
			}
		}
		return new AbstractMap.SimpleEntry<>(index, next);
	}
	
	private static boolean isInteger(String str)
	{
		try
		{
			Integer.parseInt(str);
			return true;
		}catch(NumberFormatException e)
		{
			return false;
		}
	}
}
